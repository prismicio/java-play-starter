package prismic;

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
  final static String ACCESS_TOKEN = "ACCESS_TOKEN";

  // -- Define the key name to use for storing the Prismic context token into the request arguments
  final static String PRISMIC_CONTEXT = "PRISMIC_CONTEXT";

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

  public static String getEndpoint() {
      return config("prismic.api");
  }

  // -- Fetch the API entry document
  public static Api getApiHome(String accessToken) {
    return Api.get(getEndpoint(), accessToken, CACHE, LOGGER);
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

}
