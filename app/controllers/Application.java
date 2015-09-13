package controllers;

import io.prismic.Document;
import io.prismic.Predicates;
import play.mvc.Controller;
import play.mvc.Result;
import prismic.Context;
import prismic.Prismic;
import prismic.QueryHelper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.prismic.Prismic.PREVIEW_COOKIE;
import static prismic.QueryHelper.DOCUMENT_NOT_FOUND;

public class Application extends Controller {

    @Inject
    private Prismic prismic;

    @Inject
    private QueryHelper queryHelper;

    // -- Home page
    @prismic.Action
    public Result index() {
        Context prismicContext = prismic.getContext();
        List<Document> someDocuments = prismicContext
                .getApi()
                .getForm("everything")
                .ref(prismicContext.getRef())
                .submit()
                .getResults();
        return ok(views.html.index.render(someDocuments));
    }

    // -- Document detail
    @prismic.Action
    public Result detail(String id, String slug) {
        Context prismicContext = prismic.getContext();
        Document maybeDocument = queryHelper.getDocument(prismicContext, id);
        String checked = queryHelper.checkSlug(maybeDocument, slug);
        if (checked == null) {
            return ok(views.html.detail.render(maybeDocument, prismicContext.getLinkResolver()));
        } else if (DOCUMENT_NOT_FOUND.equals(checked)) {
            return pageNotFound();
        } else {
            return redirect(controllers.routes.Application.detail(id, checked));
        }
    }

    // -- Basic Search
    @prismic.Action
    public Result search(String q) {
        List<Document> results = new ArrayList<Document>();
        Context prismicContext = prismic.getContext();
        if (q != null && !q.trim().isEmpty()) {
            results = prismicContext
                    .getApi()
                    .getForm("everything")
                    .query(Predicates.fulltext("document", q))
                    .ref(prismicContext.getRef())
                    .submit()
                    .getResults();
        }
        return ok(views.html.search.render(q, results, prismicContext.getLinkResolver()));
    }

    // -- Previews
    @prismic.Action
    public Result preview(String token) {
        String indexUrl = controllers.routes.Application.index().url();
        Context prismicContext = prismic.getContext();
        String url = prismicContext.getApi().previewSession(token, prismicContext.getLinkResolver(), indexUrl);
        response().setCookie(PREVIEW_COOKIE, token, 1800);
        return redirect(url);
    }

    // -- Page not found
    Result pageNotFound() {
        return notFound("Page not found");
    }

}
