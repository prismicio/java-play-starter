package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import views.html.*;

import io.prismic.*;
import static controllers.Prismic.*;

public class Application extends Controller {

  // -- Resolve links to documents
  final public static LinkResolver linkResolver(Api api, String ref, Http.Request request) {
    return new LinkResolver(api, ref, request);
  } 

  public static class LinkResolver extends DocumentLinkResolver {
    final Api api;
    final String ref;
    final Http.Request request;

    public LinkResolver(Api api, String ref, Http.Request request) {
      this.api = api;
      this.ref = ref;
      this.request = request;
    }

    public String resolve(Fragment.DocumentLink link) {
      return routes.Application.detail(link.getId(), link.getSlug(), ref).absoluteURL(request);
    }
  }

  // -- Page not found
  static Result pageNotFound() {
    return notFound("Page not found");
  }
  
  // -- Home page
  @Prismic.Action
  public static Result index(String ref) {
    List<Document> someDocuments = prismic().getApi().getForm("everything").ref(prismic().getRef()).submit();
    return ok(views.html.index.render(someDocuments));
  }

  // -- Document detail
  @Prismic.Action
  public static Result detail(String id, String slug, String ref) {
    Document maybeDocument = prismic().getDocument(id);
    String checked = prismic().checkSlug(maybeDocument, slug);
    if(checked == null) {
      return ok(views.html.detail.render(maybeDocument));
    }
    else if(checked == DOCUMENT_NOT_FOUND) {
      return pageNotFound();
    }
    else {
      return redirect(routes.Application.detail(id, checked, ref));
    }
  }

  // -- Basic Search
  @Prismic.Action
  public static Result search(String q, String ref) {
    List<Document> results = new ArrayList<Document>();
    if(q != null && !q.trim().isEmpty()) {
      results = prismic().getApi().getForm("everything").query("[[:d = fulltext(document, \"" + q + "\")]]").ref(prismic().getRef()).submit();
    }
    return ok(views.html.search.render(q, results));
  }
  
}
