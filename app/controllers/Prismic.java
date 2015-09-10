package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.ws.*;

import java.lang.Exception;
import java.net.*;
import java.util.*;
import java.lang.annotation.*;

import io.prismic.*;
import scala.util.control.*;

public class Prismic extends Controller {

  // -- Define the key name to use for storing the Prismic.io access token into the Play session
  private final static String ACCESS_TOKEN = "ACCESS_TOKEN";

  // -- Define the key name to use for storing the Prismic context token into the request arguments
  private final static String PRISMIC_CONTEXT = "PRISMIC_CONTEXT";

  // -- Signal for Document not found
  public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND".intern();

  // -- Cache to use (default to keep 200 JSON responses in a LRU cache)
  private final static Cache CACHE = new Cache.BuiltInCache(200);

  // -- Write debug and error messages to the Play `prismic` logger (check the configuration in application.conf)
  private final static io.prismic.Logger LOGGER = new io.prismic.Logger() {
    public void log(String level, String message) {
      if("DEBUG".equals(level)) {
        play.Logger.of("prismic").debug(message);
      }
      else if("ERROR".equals(level)) {
        play.Logger.of("prismic").error(message);
      }
      else {
        play.Logger.of("prismic").info(message);
      }
    }
  };

  // Helper method to read the Play application configuration
  private static String config(String key) {
    String value = Play.application().configuration().getString(key);
    if(value == null) {
      throw new RuntimeException("Missing configuration [" + key + "]");
    }
    return value;
  }

  // -- Fetch the API entry document
  public static Api getApiHome(String accessToken) {
    return Api.get(config("prismic.api"), accessToken, CACHE, LOGGER);
  }

  // -- A Prismic context that help to keep the reference to useful primisc.io contextual data
  public static class Context {
    final Api api;
    final String ref;
    final String accessToken;
    final LinkResolver linkResolver;

    public Context(Api api, String ref, String accessToken, LinkResolver linkResolver) {
      this.api = api;
      this.ref = ref;
      this.accessToken = accessToken;
      this.linkResolver = linkResolver;
    }

    public Api getApi() {
      return api;
    }

    public String getRef() {
      return ref;
    }

    public LinkResolver getLinkResolver() {
      return linkResolver;
    }

    // -- Helper: Retrieve a single document by Id
    public Document getDocument(String id) {
      List<Document> results = this.getApi().getForm("everything")
        .ref(this.getRef())
        .query(Predicates.at("document.id", id))
        .submit().getResults();
      if(results.size() > 0) {
        return results.get(0);
      }
      return null;
    }

    // -- Helper: Retrieve several documents by Id
    public List<Document> getDocuments(List<String> ids) {
      if(ids.isEmpty()) {
        return new ArrayList<Document>();
      } else {
        return this.getApi()
          .getForm("everything")
          .query(Predicates.any("document.id", ids))
          .ref(this.getRef())
          .submit()
          .getResults();
      }
    }

    // -- Helper: Retrieve a single document from its bookmark
    public Document getBookmark(String bookmark) {
      String id = this.getApi().getBookmarks().get(bookmark);
      if(id != null) {
        return getDocument(id);
      } else {
        return null;
      }
    }

    // -- Helper: Check if the slug is valid and return to the most recent version to redirect to if needed, or return DOCUMENT_NOT_FOUND if there is no match
    public String checkSlug(Document document, String slug) {
      if(document != null) {
        if(document.getSlug().equals(slug)) {
          return null;
        }
        if(document.getSlugs().contains(slug)) {
          return document.getSlug();
        }
      }
      return DOCUMENT_NOT_FOUND;
    }

  }

  // -- Retrieve the Prismic Context from a request handled by an built using Prismic.action
  public static Prismic.Context prismic() {
    Context ctx = (Prismic.Context)Http.Context.current().args.get(PRISMIC_CONTEXT);
    if(ctx == null) {
      throw new RuntimeException("No Context API found - Is it a @Prismic.Action?");
    }
    return ctx;
  }

  // -- Prismic Action annotation
  @With(Prismic.ActionImpl.class)
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public static @interface Action {}

  public static class ActionImpl extends play.mvc.Action<Prismic.Action> {

    public F.Promise<Result> call(Http.Context ctx) throws Throwable {
      // Retrieve the accessToken from the Play session or from the configuration
      String accessToken = ctx.session().get(ACCESS_TOKEN);
      if(accessToken == null) {
        accessToken = Play.application().configuration().getString("prismic.token");
      }

      // Retrieve the API
      Api api = getApiHome(accessToken);

      // Use the ref from the preview cookie, experiment cookie or master
      String ref = api.getMaster().getRef();
      Http.Cookie previewCookie = ctx.request().cookie(io.prismic.Prismic.PREVIEW_COOKIE);
      Http.Cookie experimentCookie = ctx.request().cookie(io.prismic.Prismic.EXPERIMENTS_COOKIE);
      if (previewCookie != null) {
        ref = previewCookie.value();
      } else if (experimentCookie != null) {
        ref = api.getExperiments().refFromCookie(experimentCookie.value());
      }

      // Create the Prismic context
      Prismic.Context prismicContext = new Prismic.Context(api, ref, accessToken, Application.linkResolver(api, ctx.request()));

      // Strore it for future use
      ctx.args.put(PRISMIC_CONTEXT, prismicContext);

      // Go!
      try {
        return delegate.call(ctx);
      } catch (Exception e) {
        if ("1".equals(flash("clearing"))) {
          // Prevent infinite redirect loop if the exception is not due to the preview cookie
          return delegate.call(ctx);
        } else {
          response().discardCookie(io.prismic.Prismic.PREVIEW_COOKIE);
          flash("clearing", "1");
          return F.Promise.pure(redirect(routes.Application.index()));
        }
      }
    }

  }

  // --
  // -- Previews
  // --
  @Prismic.Action
  public Result preview(String token) {
    String indexUrl = controllers.routes.Application.index().url();
    String url = prismic().api.previewSession(token, prismic().getLinkResolver(), indexUrl);
    response().setCookie(io.prismic.Prismic.PREVIEW_COOKIE, token, 1800);
    return redirect(url);
  }

}
