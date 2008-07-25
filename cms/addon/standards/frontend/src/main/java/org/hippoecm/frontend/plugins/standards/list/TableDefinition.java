package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDefinition {
    private List<ListColumn> columns;
    private Map<String, Comparator> comparators;
    
    public TableDefinition(List<ListColumn> columnList) {
        columns = new ArrayList<ListColumn>();
        comparators = new HashMap<String, Comparator>();
        for (ListColumn column: columnList) {
            columns.add(column);
            comparators.put(column.getSortProperty(), column.getComparator());
        }
    }
    
    public Comparator getComparator(String sortProperty) {
        return comparators.get(sortProperty);
    }
    
    public ListColumn[] asArray() {
        return (ListColumn[])(columns.toArray(new ListColumn[columns.size()]));
    }
}
