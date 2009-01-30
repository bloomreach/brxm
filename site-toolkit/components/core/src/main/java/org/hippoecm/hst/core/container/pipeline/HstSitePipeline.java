package org.hippoecm.hst.core.container.pipeline;

import org.hippoecm.hst.core.request.HstRequestContext;

public class HstSitePipeline implements Pipeline
{
    protected String name;
    protected Valve [] valves;
    
    public HstSitePipeline(String name, Valve [] valves) throws Exception
    {
        this.name = name;
        this.valves = valves;
    }

    public String getName()
    {
        return this.name;
    }

    public void initialize() throws Exception
    {
    }

    public void invoke(HstRequestContext request) throws Exception
    {
        new Invocation(valves).invokeNext(request);
    }
    
    private static final class Invocation implements ValveContext
    {

        private final Valve[] valves;

        private int at = 0;

        public Invocation(Valve[] valves)
        {
            this.valves = valves;
        }

        public void invokeNext(HstRequestContext request) throws Exception
        {
            if (at < valves.length)
            {
                Valve next = valves[at];
                at++;
                next.invoke(request, this);
            }
        }
    }
}
