package prismic;

import io.prismic.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static prismic.QueryHelper.DOCUMENT_NOT_FOUND;

public class QueryHelperTest {

    private QueryHelper queryHelper = new QueryHelper();

    @Test
    public void whenGetDocument_thenShouldReturnNull() throws Exception {
        // Given
        Context context = mock(Context.class);
        Api api = mock(Api.class);
        Form.SearchForm form = mock(Form.SearchForm.class);
        Response response = mock(Response.class);

        when(context.getApi()).thenReturn(api);
        when(context.getRef()).thenReturn("ref");
        when(api.getForm(anyString())).thenReturn(form);
        when(form.ref("ref")).thenReturn(form);
        when(form.query((Predicate[]) anyVararg())).thenReturn(form);
        when(form.submit()).thenReturn(response);
        when(response.getResults()).thenReturn(Collections.<Document>emptyList());

        // When
        Document actual = queryHelper.getDocument(context, "document-id");

        // Then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void whenGetDocument_thenShouldReturnFirstFoundDocument() throws Exception {
        // Given
        Context context = mock(Context.class);
        Api api = mock(Api.class);
        Form.SearchForm form = mock(Form.SearchForm.class);
        Response response = mock(Response.class);
        Document expectedDocument = mock(Document.class);
        Document otherDocument = mock(Document.class);

        when(context.getApi()).thenReturn(api);
        when(context.getRef()).thenReturn("ref");
        when(api.getForm(anyString())).thenReturn(form);
        when(form.ref("ref")).thenReturn(form);
        when(form.query((Predicate[]) anyVararg())).thenReturn(form);
        when(form.submit()).thenReturn(response);
        when(response.getResults()).thenReturn(Arrays.asList(expectedDocument, otherDocument));

        // When
        Document actual = queryHelper.getDocument(context, "document-id");

        // Then
        assertThat(actual, is(expectedDocument));
    }

    @Test
    public void givenIdsAreEmpty_whenGetDocuments_thenShouldReturnAnEmptyList() throws Exception {
        // Given
        Context context = mock(Context.class);

        // When
        List<Document> actual = queryHelper.getDocuments(context, Collections.<String>emptyList());

        // Then
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(0));
    }

    @Test
    public void givenIdsAreNotEmpty_whenGetDocuments_thenShouldReturnAListOfCorrespondingDocuments() throws Exception {
        // Given
        Context context = mock(Context.class);
        Api api = mock(Api.class);
        Form.SearchForm form = mock(Form.SearchForm.class);
        Response response = mock(Response.class);
        Document aDocument = mock(Document.class);
        Document otherDocument = mock(Document.class);

        when(context.getApi()).thenReturn(api);
        when(context.getRef()).thenReturn("ref");
        when(api.getForm(anyString())).thenReturn(form);
        when(form.ref("ref")).thenReturn(form);
        when(form.query((Predicate[]) anyVararg())).thenReturn(form);
        when(form.submit()).thenReturn(response);
        when(response.getResults()).thenReturn(Arrays.asList(aDocument, otherDocument));

        // When
        List<Document> actual = queryHelper.getDocuments(context, Arrays.asList("doc-1", "doc-2"));

        // Then
        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), is(2));
    }

    @Test
    public void givenDocumentIsNull_whenCheckSlug_thenShouldReturnDocumentNotFound() throws Exception {
        // Given
        String slug = "slug";

        // When
        String actual = queryHelper.checkSlug(null, slug);

        // Then
        assertThat(actual, is(DOCUMENT_NOT_FOUND));
    }

    @Test
    public void givenDocumentSlugIsTheLastOne_whenCheckSlug_thenShouldReturnNull() throws Exception {
        // Given
        Document document = mock(Document.class);
        when(document.getSlug()).thenReturn("slug");

        // When
        String actual = queryHelper.checkSlug(document, "slug");

        // Then
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void givenDocumentContainsSlug_whenCheckSlug_thenReturnLatestSlug() throws Exception {
        // Given
        Document document = mock(Document.class);
        List<String> slugs = mock(List.class);

        when(slugs.contains("old-slug")).thenReturn(true);
        when(document.getSlug()).thenReturn("new-slug");
        when(document.getSlugs()).thenReturn(slugs);

        // When
        String actual = queryHelper.checkSlug(document, "old-slug");

        // Then
        assertThat(actual, is("new-slug"));
    }
}