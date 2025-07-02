package kz.home.RelaySmartSystems;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public class Utils {
    public static String removeFieldsJSON(String json, String... fieldsToRemove) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        String cleanedJson = null;
        try {
            root = mapper.readTree(json);
            removeFieldsRecursive(root, fieldsToRemove);
            cleanedJson = mapper.writer().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return cleanedJson;
    }

    private static void removeFieldsRecursive(JsonNode node, String... fieldsToRemove) {
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            for (String field : fieldsToRemove) {
                objNode.remove(field);
            }
            Iterator<Map.Entry<String, JsonNode>> iter = objNode.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                removeFieldsRecursive(entry.getValue(), fieldsToRemove);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                removeFieldsRecursive(item, fieldsToRemove);
            }
        }
    }
}
