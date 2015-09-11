package prismic;

import io.prismic.Document;
import io.prismic.Predicates;

import java.util.ArrayList;
import java.util.List;

public class QueryHelper {

    // -- Signal for Document not found
    public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";

    // -- Helper: Retrieve a single document by Id
    public Document getDocument(Context context, String id) {
        List<Document> results = context.getApi().getForm("everything")
                .ref(context.getRef())
                .query(Predicates.at("document.id", id))
                .submit().getResults();
        if(results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    // -- Helper: Retrieve several documents by Id
    public List<Document> getDocuments(Context context, List<String> ids) {
        if(ids.isEmpty()) {
            return new ArrayList<Document>();
        } else {
            return context.getApi()
                    .getForm("everything")
                    .query(Predicates.any("document.id", ids))
                    .ref(context.getRef())
                    .submit()
                    .getResults();
        }
    }

    // -- Helper: Retrieve a single document from its bookmark
    public Document getBookmark(Context context, String bookmark) {
        String id = context.getApi().getBookmarks().get(bookmark);
        if(id != null) {
            return getDocument(context, id);
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
