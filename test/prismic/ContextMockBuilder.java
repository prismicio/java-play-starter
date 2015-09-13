package prismic;

import io.prismic.*;

import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContextMockBuilder {

    private Context context = mock(Context.class);
    private Api api = mock(Api.class);
    private Ref ref = mock(Ref.class);
    private Form.SearchForm form = mock(Form.SearchForm.class);
    private Response response = mock(Response.class);

    public ContextMockBuilder() {
        // Context
        when(context.getApi()).thenReturn(api);
        when(context.getRef()).thenReturn("ref");

        // Api
        when(api.getMaster()).thenReturn(ref);
        when(api.getForm(anyString())).thenReturn(form);

        // Ref
        when(ref.getRef()).thenReturn("ref");

        // Form
        when(form.ref(anyString())).thenReturn(form);
        when(form.query((Predicate[]) anyVararg())).thenReturn(form);
        when(form.submit()).thenReturn(response);

        // Response
        when(response.getResults()).thenReturn(Collections.<Document>emptyList());
    }

    public ContextMockBuilder withResults(List<Document> results) {
        when(response.getResults()).thenReturn(results);
        return this;
    }

    public Context build() {
        return context;
    }
}
