import io.prismic.Document;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import play.twirl.api.Content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {

    @Test 
    public void simpleCheck() {
        int a = 1 + 1;
        assertThat(a, is(2));
    }
    
    @Test
    public void renderTemplate() {
        Document document = new Document("id", "uid", "StructuredText", "href", Collections.<String>emptySet(), Arrays.asList("My super document"), Collections.emptyMap());
        List<Document> someDocuments = Arrays.asList(document);
        Content html = views.html.index.render(someDocuments);
        assertThat(html.contentType(), is("text/html"));
        assertThat(html.body(), containsString("My super document"));
    }
  
   
}
