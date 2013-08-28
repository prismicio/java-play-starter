package controllers;

import play.*;
import play.mvc.*;
import play.libs.*;

import java.net.*;
import java.util.*;
import java.lang.annotation.*;

import io.prismic.*;

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

  // Compute the callback URL to use for the OAuth worklow
  public static String callbackUrl(Http.Request request) {
    String[] referer = request.headers().get("referer");
    if(referer == null) {
      referer = new String[0];
    }
    return routes.Prismic.callback(null, referer.length > 0 ? referer[0] : null).absoluteURL(request);
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
    final DocumentLinkResolver linkResolver;

    public Context(Api api, String ref, String accessToken, DocumentLinkResolver linkResolver) {
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

    public String getAccessToken() {
      return accessToken;
    }

    public DocumentLinkResolver getLinkResolver() {
      return linkResolver;
    }

    public String maybeRef() {
      if(ref.equals(api.getMaster().getRef())) {
        return null;
      }
      return ref;
    }

    public boolean hasPrivilegedAccess() {
      return accessToken != null;
    }

    // -- Helper: Retrieve a single document by Id
    public Document getDocument(String id) {
      List<Document> results = this.getApi().getForm("everything").query("[[:d = at(document.id, \"" + id + "\")]]").ref(this.getRef()).submit();
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
        StringBuilder q = new StringBuilder();
        q.append("[[:d = any(document.id, [");
        String sep = "";
        for(String id: ids) {
          q.append(sep + "\"" + id + "\"");
          sep = ",";
        }
        q.append("\"]]");
        return this.getApi().getForm("everything").query(q.toString()).ref(this.getRef()).submit();
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

    public F.Promise<SimpleResult> call(Http.Context ctx) throws Throwable {
      // Retrieve the accessToken from the Play session or from the configuration
      String accessToken = ctx.session().get(ACCESS_TOKEN);
      if(accessToken == null) {
        accessToken = Play.application().configuration().getString("prismic.token");
      }

      // Retrieve the API
      Api api = getApiHome(accessToken);

      // Reuse the ref from the incoming request or use master ref
      String ref = ctx.request().getQueryString("ref");
      if(ref == null || ref.trim().isEmpty()) {
        ref = api.getMaster().getRef();
      }

      // Create the Prismic context
      Prismic.Context prismicContext = new Prismic.Context(api, ref, accessToken, Application.linkResolver(api, ref, ctx.request()));

      // Strore it for future use
      ctx.args.put(PRISMIC_CONTEXT, prismicContext);

      // Go!
      return delegate.call(ctx);
    }

  }

  // --
  // -- OAuth actions
  // --

  @Prismic.Action
  public static Result signin() throws Exception {
    StringBuilder url = new StringBuilder();
    url.append(prismic().getApi().getOAuthInitiateEndpoint());
    url.append("?");
    url.append("client_id=");
    url.append(URLEncoder.encode(config("prismic.clientId"), "utf-8"));
    url.append("&redirect_uri=");
    url.append(URLEncoder.encode(callbackUrl(ctx().request()), "utf-8"));
    url.append("&scope=");
    url.append(URLEncoder.encode("master+releases", "utf-8"));
    return redirect(url.toString());
  }

  public static Result signout() {
    session().clear();
    return redirect(routes.Application.index(null));
  }

  @Prismic.Action
  public static Result callback(String code, String redirect_uri) throws Exception {
    StringBuilder body = new StringBuilder();
    body.append("grant_type=");
    body.append(URLEncoder.encode("authorization_code", "utf-8"));
    body.append("&code=");
    body.append(URLEncoder.encode(code, "utf-8"));
    body.append("&redirect_uri=");
    body.append(URLEncoder.encode(callbackUrl(ctx().request()), "utf-8"));
    body.append("&client_id=");
    body.append(URLEncoder.encode(config("prismic.clientId"), "utf-8"));
    body.append("&client_secret=");
    body.append(URLEncoder.encode(config("prismic.clientSecret"), "utf-8")); 
    WS.Response response = WS.url(prismic().getApi().getOAuthTokenEndpoint()).setHeader(CONTENT_TYPE, "application/x-www-form-urlencoded").post(body.toString()).get();
    if(response.getStatus() == 200) {
      String accessToken = response.asJson().path("access_token").asText();
      String redirectUrl = redirect_uri;
      if(redirectUrl == null) {
        redirectUrl = routes.Application.index(null).url();
      }
      session(ACCESS_TOKEN, accessToken);
      return redirect(redirectUrl);
    }
    else {
      play.Logger.of("prismic").error("Can't retrieve the OAuth token for code " + code + ": " + response.getStatus() + " " + response.getBody());
      return unauthorized("Can't sign you in");
    }
  }

}