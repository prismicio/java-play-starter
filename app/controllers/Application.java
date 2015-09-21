package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import views.html.*;

import io.prismic.*;
import static prismic.Prismic.*;

public class Application extends Controller {

  // -- Home page
  @prismic.Action
  public Result index() {
    List<Document> someDocuments = prismic().getApi().getForm("everything").ref(prismic().getRef()).submit().getResults();
    return ok(views.html.index.render(someDocuments));
  }

  // -- Document detail
  @prismic.Action
  public Result detail(String id, String slug) {
    Document maybeDocument = prismic().getDocument(id);
    String checked = prismic().checkSlug(maybeDocument, slug);
    if(checked == null) {
      return ok(views.html.detail.render(maybeDocument, prismic().getLinkResolver()));
    }
    else if(prismic.Prismic.DOCUMENT_NOT_FOUND.equals(checked)) {
      return pageNotFound();
    }
    else {
      return redirect(routes.Application.detail(id, checked));
    }
  }

  // -- Basic Search
  @prismic.Action
  public Result search(String q) {
    List<Document> results = new ArrayList<Document>();
    if(q != null && !q.trim().isEmpty()) {
      results = prismic().getApi().getForm("everything").query(Predicates.fulltext("document", q)).ref(prismic().getRef()).submit().getResults();
    }
    return ok(views.html.search.render(q, results, prismic().getLinkResolver()));
  }

  // ---- Links

  // -- Resolve links to documents
  public static LinkResolver linkResolver(Api api, Http.Request request) {
    return new LinkResolver(api, request);
  }

  public static class LinkResolver extends SimpleLinkResolver {
    final Api api;
    final Http.Request request;

    public LinkResolver(Api api, Http.Request request) {
      this.api = api;
      this.request = request;
    }

    public String resolve(Fragment.DocumentLink link) {
      return routes.Application.detail(link.getId(), link.getSlug()).absoluteURL(request);
    }
  }

  // -- Page not found
  static Result pageNotFound() {
    return notFound("Page not found");
  }

  // --
  // -- Previews
  // --
  @prismic.Action
  public Result preview(String token) {
    String indexUrl = controllers.routes.Application.index().url();
    String url = prismic().getApi().previewSession(token, prismic().getLinkResolver(), indexUrl);
    response().setCookie(io.prismic.Prismic.PREVIEW_COOKIE, token, 1800);
    return redirect(url);
  }

}
