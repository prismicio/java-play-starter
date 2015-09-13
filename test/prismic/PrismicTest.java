package prismic;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import play.mvc.Http;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static prismic.Prismic.PRISMIC_CONTEXT;

public class PrismicTest {

    private Prismic prismic = new Prismic();

    @Test(expected = RuntimeException.class)
    public void givenNoPrismicContextWasDefinedInPlaySession_whenGetContext_thenThrowAnException() throws Exception {
        // Given
        Http.Request mockRequest = mock(Http.Request.class);
        Http.Context context = Mockito.mock(Http.Context.class);
        context.args = new HashMap<String, Object>();
        when(context.request()).thenReturn(mockRequest);
        Http.Context.current.set(context);

        Http.Context.current().args.put(PRISMIC_CONTEXT, null);

        // When
        prismic.getContext();

        // Then
    }

    @Test
    public void givenAPrismicContextWasDefinedInPlaySession_whenGetContext_thenShouldReturnTheRetrievedContext() throws Exception {
        // Given
        Http.Request mockRequest = mock(Http.Request.class);
        Http.Context context = Mockito.mock(Http.Context.class);
        context.args = new HashMap<String, Object>();
        when(context.request()).thenReturn(mockRequest);
        Http.Context.current.set(context);
        Context prismicContext = mock(Context.class);

        Http.Context.current().args.put(PRISMIC_CONTEXT, prismicContext);

        // When
        Context actual = prismic.getContext();

        // Then
        Assert.assertThat(actual, is(prismicContext));
    }
}