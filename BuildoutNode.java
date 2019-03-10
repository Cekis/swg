package swg;

import java.util.HashMap;
import java.util.Map;

public class BuildoutNode {

    private Map<String, Object> columnData = new HashMap<String, Object>();

    public BuildoutNode() {}

    public void addValue(String name, Object value){
        columnData.put(name, value);
    }
    public void removeValue(String name){
        columnData.remove(name);
    }
    public Object getValue(String name){
        return columnData.get(name);
    }
    public void setTemplateName(String template) {
        columnData.put("object", template);
    }
}
