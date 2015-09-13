package prismic;

import io.prismic.Api;
import io.prismic.Fragment;
import org.junit.Assert;
import org.junit.Test;
import play.mvc.Http;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LinkResolverTest {

    @Test
    public void testResolve() throws Exception {
        // Given
        Api api = mock(Api.class);
        Http.Request request = mock(Http.Request.class);
        when(request.host()).thenReturn("localhost");
        Fragment.DocumentLink link = mock(Fragment.DocumentLink.class);
        LinkResolver linkResolver = new LinkResolver(api, request);

        when(link.getId()).thenReturn("id");
        when(link.getSlug()).thenReturn("slug");

        // When
        String actual = linkResolver.resolve(link);

        // Then
        Assert.assertThat(actual, is("http://localhost/documents/id/slug"));
    }

}