package com.hesong.jsonrpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class JsonrpcHandler {
    private static Logger log = Logger.getLogger(JsonrpcHandler.class);
    
    private Object handler;
    private ObjectMapper mapper;

    public Object getHandler() {
        return handler;
    }

    public void setHandler(Object handler) {
        this.handler = handler;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonrpcHandler(Object handler) {
        super();
        this.handler = handler;
        mapper = new ObjectMapper();
    }

    public String handle(String jsonrpc) {
        log.info("Handle json: "+jsonrpc);
        try {
            return hadleNode(mapper.readValue(jsonrpc, JsonNode.class));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return createErrorResponse("2.0", null, 9999, e.toString(), null);
        }
    }

    private String hadleNode(JsonNode node) {
        // handle objects
        if (node.isObject()) {
            return handleObject(ObjectNode.class.cast(node));

            // handle arrays
        } else if (node.isArray()) {
            handleArray(ArrayNode.class.cast(node));
            return "";
        }
        return "";
    }

    private void handleArray(ArrayNode cast) {
        // TODO Auto-generated method stub

    }

    private String handleObject(ObjectNode node) {
        String callBack;
        // validate request
        if (!node.has("jsonrpc") || !node.has("method")) {
            log.info("Result message, do nothing.");
            return null;
        }

        // parse request
        String jsonRpc = node.get("jsonrpc").getTextValue();
        String methodName = node.get("method").getTextValue();
        String id = node.get("id").getTextValue();
        JsonNode params = node.get("params");
        log.info("Json: "+ jsonRpc + " " + methodName + " "+id+" "+params);
        
        int paramCount = (params != null) ? params.size() : 0;

        // find methods
        Set<Method> methods = findMethods(methodName);

        // method not found
        if (methods.isEmpty()) {
            callBack = createErrorResponse(jsonRpc, id, -32601,
                    "Method not found", null);
            return callBack;
        }

        Iterator<Method> itr = methods.iterator();
        while (itr.hasNext()) {
            Method method = itr.next();
            if (method.getParameterTypes().length != paramCount) {
                itr.remove();
            }
        }
        
        if (methods.size() == 0) {
            return createErrorResponse(jsonRpc, id, -32602,
                    "Unknown method or invalid method parameters.", null);
        }
        
        // choose a method
        Method method = null;
        List<JsonNode> paramNodes = new ArrayList<JsonNode>();

        // handle param arrays, no params, and single methods
        
        if (paramCount == 0 || params.isArray() || (methods.size() == 1)) {
            method = methods.iterator().next();
            for (int i = 0; i < paramCount; i++) {
                paramNodes.add(params.get(i));
            }

            // handle named params
        } else if (params.isObject()) {

            // loop through each method
            for (Method m : methods) {

                // get method annotations
                Annotation[][] annotations = m.getParameterAnnotations();
                boolean found = true;
                List<JsonNode> namedParams = new ArrayList<JsonNode>();
                for (int i = 0; i < annotations.length; i++) {

                    // look for param name annotations
                    String paramName = null;
                    for (int j = 0; j < annotations[i].length; j++) {
                        if (!JsonrpcParamName.class
                                .isInstance(annotations[i][j])) {
                            continue;
                        } else {
                            paramName = JsonrpcParamName.class.cast(
                                    annotations[i][j]).value();
                            continue;
                        }
                    }

                    // bail if param name wasn't found
                    if (paramName == null) {
                        found = false;
                        break;

                        // found it by name
                    } else if (params.has(paramName)) {
                        namedParams.add(params.get(paramName));
                    }
                }

                // did we find it?
                if (found) {
                    method = m;
                    paramNodes.addAll(namedParams);
                    break;
                }
            }
        }

        // invalid parameters
        if (method == null) {
            callBack = createErrorResponse(jsonRpc, id, -32602,
                    "Invalid method parameters.", null);
            return callBack;
        }

        // invoke the method
        JSONObject result = null;
        ObjectNode error = null;
        try {
            result = invoke(method, paramNodes);
        } catch (Exception e) {
            error = mapper.createObjectNode();
            error.put("code", 0);
            error.put("message", e.getLocalizedMessage());
            //error.put("data", mapper.valueToTree(e));
        }

        // bail if notification request
        if (id == null) {
            return "id is null";
        }

        // create response
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", jsonRpc);
        response.put("id", id);
        
        if (error != null) {
            response.put("error", error);
            return response.toString();
        }
        if (result != null) {
            int errcode = result.getInt("errcode");
            if (errcode==0) {
                response.put("result", 0);
            } else if (errcode != 0) {
                ObjectNode on = mapper.createObjectNode();
                on.put("code", errcode);
                on.put("message", result.getString("errmsg"));
                response.put("error", on);
            }
        } else {
            return createErrorResponse(jsonRpc, id, -32602,
                    "Invalid method parameters.", null);
        }

        callBack = response.toString();
        return callBack;
    }

    /**
     * Finds methods with the given name on the {@code handler}.
     * 
     * @param name
     *            the name
     * @return the methods
     */
    private Set<Method> findMethods(String name) {
        Set<Method> ret = new HashSet<Method>();
        for (Method method : getHandler().getClass().getMethods()) {
            if (method.getName().equals(name)) {
                ret.add(method);
            }
        }
        return ret;
    }

    /**
     * Invokes the given method on the {@code handler} passing the given params
     * (after converting them to beans\objects) to it.
     * 
     * @param m
     *            the method to invoke
     * @param params
     *            the params to pass to the method
     * @return the return value (or null if no return)
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("deprecation")
    private JSONObject invoke(Method m, List<JsonNode> params)
            throws JsonParseException, JsonMappingException, IOException,
            IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {

        // convert the parameters
        Object[] convertedParams = new Object[params.size()];
        Type[] parameterTypes = m.getGenericParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            convertedParams[i] = mapper.treeToValue(params.get(i), TypeFactory
                    .type(parameterTypes[i]).getRawClass());
        }

        // invoke the method
        Object result = m.invoke(handler, convertedParams);
        return (m.getGenericReturnType() != null) ? (JSONObject)result : null;
    }

    /**
     * Convenience method for creating an error response
     * 
     * @param jsonRpc
     *            the jsonrpc string
     * @param id
     *            the id
     * @param code
     *            the error code
     * @param message
     *            the error message
     * @param data
     *            the error data (if any)
     * @return the error response
     */
    private String createErrorResponse(String jsonRpc, String id, int code,
            String message, Object data) {
        ObjectNode response = mapper.createObjectNode();
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.put("data", mapper.valueToTree(data));
        }
        response.put("jsonrpc", jsonRpc);
        response.put("id", id);
        response.put("error", error);
        return response.toString();
    }
}
