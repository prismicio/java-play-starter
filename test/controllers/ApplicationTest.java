package controllers;

import io.prismic.Api;
import io.prismic.Document;
import io.prismic.Ref;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import play.mvc.Result;
import play.test.WithApplication;
import play.twirl.api.Content;
import prismic.Context;
import prismic.ContextMockBuilder;
import prismic.Prismic;
import prismic.QueryHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.route;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationTest {

    @InjectMocks
    private Application application;

    @Mock
    private Prismic prismic;

    @Mock
    private QueryHelper queryHelper;

    @Test
    public void givenThereIsNoDocument_whenShowIndexPage_thenNoDocumentIsDisplayed() {
        // Given
        Context context = new ContextMockBuilder().build();
        when(prismic.getContext()).thenReturn(context);

        // When
        Result result = application.index();

        // Then
        assertThat(result.charset(), is("utf-8"));
        assertThat(result.contentType(), is("text/html"));
        assertThat(contentAsString(result), containsString("No documents found"));
    }

    @Test
    public void givenThereIsOneDocument_whenShowIndexPage_thenTheLinkToThePageIsDisplayed() {
        // Given
        Document document = mock(Document.class);
        when(document.getId()).thenReturn("doc-id");
        when(document.getSlug()).thenReturn("document-slug");
        Context context = new ContextMockBuilder().withResults(singletonList(document)).build();
        when(prismic.getContext()).thenReturn(context);

        // When
        Result result = application.index();

        // Then
        String page = contentAsString(result);
        assertThat(page, containsString("One document found"));
        assertThat(page, containsString("<a href=\"/documents/doc-id/document-slug\">"));
    }

    @Test
    public void givenThereAreSeveralDocuments_whenShowIndexPage_thenTheLinksToThePagesAreDisplayed() {
        // Given
        Document doc1 = mock(Document.class);
        when(doc1.getId()).thenReturn("1");
        when(doc1.getSlug()).thenReturn("abc");

        Document doc2 = mock(Document.class);
        when(doc2.getId()).thenReturn("2");
        when(doc2.getSlug()).thenReturn("def");

        Document doc3 = mock(Document.class);
        when(doc3.getId()).thenReturn("3");
        when(doc3.getSlug()).thenReturn("ghi");

        Context context = new ContextMockBuilder().withResults(asList(doc1, doc2, doc3)).build();
        when(prismic.getContext()).thenReturn(context);

        // When
        Result result = application.index();

        // Then
        String page = contentAsString(result);
        assertThat(page, containsString("3 documents found"));
        assertThat(page, containsString("<a href=\"/documents/1/abc\">"));
        assertThat(page, containsString("<a href=\"/documents/2/def\">"));
        assertThat(page, containsString("<a href=\"/documents/3/ghi\">"));
    }

    @Test
    public void renderTemplate() {
        Document document = new Document("id", "uid", "StructuredText", "href", Collections.<String>emptySet(), asList("My super document"), Collections.emptyMap());
        List<Document> someDocuments = asList(document);
        Content html = views.html.index.render(someDocuments);
        assertThat(html.contentType(), is("text/html"));
        assertThat(html.body(), containsString("My super document"));
    }
  
   
}
