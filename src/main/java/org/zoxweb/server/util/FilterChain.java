package org.zoxweb.server.util;


import org.zoxweb.shared.util.Const.FunctionStatus;

import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class FilterChain<T>
implements Function<T, FunctionStatus>
{



    private final ListIterator<Function<T, FunctionStatus>> filtersIter;
    private final Consumer<T> handler;

    public FilterChain(List<Function<T, FunctionStatus >> filters, Consumer<T> handler)
    {
        filtersIter = filters.listIterator();
        this.handler = handler;
    }

    public FunctionStatus apply(T data)
    {
        if (filtersIter.hasNext()) {
            FunctionStatus fs = filtersIter.next().apply(data);
            switch (fs)
            {

                case PARTIAL:
                case CONTINUE:
                case COMPLETED:
                    apply(data);
                    break;
                case ERROR:
                    return FunctionStatus.ERROR;

            }

        }
        else
            handler.accept(data);

        return FunctionStatus.COMPLETED;
    }




}
