/*
 * Copyright 2006-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.kubernetes.command;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.kubernetes.message.KubernetesMessageHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author Christoph Deppisch
 * @since 2.7
 */
public abstract class AbstractKubernetesCommand<R, T extends AbstractKubernetesCommand> implements KubernetesCommand {

    /** Logger */
    protected Logger log = LoggerFactory.getLogger(getClass());

    /** Self reference for generics support */
    private final T self;

    /** Command name */
    private final String name;

    /** Command parameters */
    private Map<String, Object> parameters = new HashMap<>();

    /** Command result if any */
    private CommandResult<R> commandResult;

    /** Optional command result validation */
    private CommandResultCallback<R> resultCallback;

    /**
     * Default constructor initializing the command name.
     * @param name
     */
    public AbstractKubernetesCommand(String name) {
        this.name = String.format("kubernetes:%s", name);
        this.self = (T) this;
    }

    /**
     * Checks existence of command parameter.
     * @param parameterName
     * @return
     */
    protected boolean hasParameter(String parameterName) {
        return getParameters().containsKey(parameterName);
    }

    /**
     * Gets the kubernetes command parameter.
     * @return
     */
    protected String getParameter(String parameterName, TestContext context) {
        if (getParameters().containsKey(parameterName)) {
            return context.replaceDynamicContentInString(getParameters().get(parameterName).toString());
        } else {
            throw new CitrusRuntimeException(String.format("Missing kubernetes command parameter '%s'", parameterName));
        }
    }

    @Override
    public CommandResult<R> getCommandResult() {
        return commandResult;
    }

    /**
     * Sets the command result if any.
     * @param commandResult
     */
    protected void setCommandResult(CommandResult<R> commandResult) {
        this.commandResult = commandResult;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Sets the command parameters.
     * @param parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * Adds command parameter to current command.
     * @param name
     * @param value
     * @return
     */
    public T withParam(String name, String value) {
        parameters.put(name, value);
        return self;
    }

    /**
     * Adds validation callback with command result.
     * @param callback
     * @return
     */
    public T validateCommandResult(CommandResultCallback<R> callback) {
        this.resultCallback = callback;
        return self;
    }

    /**
     * Gets the result validation callback.
     * @return
     */
    public CommandResultCallback<R> getResultCallback() {
        return resultCallback;
    }

    /**
     * Reads labels from expression string.
     * @param labelExpression
     * @param context
     * @return
     */
    protected Map<String, String> getLabels(String labelExpression, TestContext context) {
        Map<String, String> labels = new HashMap<>();

        Set<String> values = StringUtils.commaDelimitedListToSet(labelExpression);
        for (String item : values) {
            if (item.contains("!=")) {
                continue;
            } else if (item.contains("=")) {
                labels.put(context.replaceDynamicContentInString(item.substring(0, item.indexOf("="))), context.replaceDynamicContentInString(item.substring(item.indexOf("=") + 1)));
            } else if (!item.startsWith("!")) {
                labels.put(context.replaceDynamicContentInString(item), null);
            }
        }

        return labels;
    }

    /**
     * Reads without labels from expression string.
     * @param labelExpression
     * @param context
     * @return
     */
    protected Map<String, String> getWithoutLabels(String labelExpression, TestContext context) {
        Map<String, String> labels = new HashMap<>();

        Set<String> values = StringUtils.commaDelimitedListToSet(labelExpression);
        for (String item : values) {
            if (item.contains("!=")) {
                labels.put(context.replaceDynamicContentInString(item.substring(0, item.indexOf("!="))), context.replaceDynamicContentInString(item.substring(item.indexOf("!=") + 2)));
            } else if (item.startsWith("!")) {
                labels.put(context.replaceDynamicContentInString(item.substring(1)), null);
            }
        }

        return labels;
    }

    /**
     * Sets the label parameter.
     * @param key
     * @param value
     * @return
     */
    public T label(String key, String value) {
        if (!hasParameter(KubernetesMessageHeaders.LABEL)) {
            getParameters().put(KubernetesMessageHeaders.LABEL, key + "=" + value);
        } else {
            getParameters().put(KubernetesMessageHeaders.LABEL, getParameters().get(KubernetesMessageHeaders.LABEL) + "," + key + "=" + value);
        }
        return self;
    }

    /**
     * Sets the label parameter.
     * @param key
     * @return
     */
    public T label(String key) {
        if (!hasParameter(KubernetesMessageHeaders.LABEL)) {
            getParameters().put(KubernetesMessageHeaders.LABEL, key);
        } else {
            getParameters().put(KubernetesMessageHeaders.LABEL, getParameters().get(KubernetesMessageHeaders.LABEL) + "," + key);
        }
        return self;
    }

    /**
     * Sets the namespace parameter.
     * @param key
     * @return
     */
    public T namespace(String key) {
        getParameters().put(KubernetesMessageHeaders.NAMESPACE, key);
        return self;
    }

    /**
     * Sets the name parameter.
     * @param key
     * @return
     */
    public T name(String key) {
        getParameters().put(KubernetesMessageHeaders.NAME, key);
        return self;
    }

    /**
     * Sets the without label parameter.
     * @param key
     * @param value
     * @return
     */
    public T withoutLabel(String key, String value) {
        if (!hasParameter(KubernetesMessageHeaders.LABEL)) {
            getParameters().put(KubernetesMessageHeaders.LABEL, key + "!=" + value);
        } else {
            getParameters().put(KubernetesMessageHeaders.LABEL, getParameters().get(KubernetesMessageHeaders.LABEL) + "," + key + "!=" + value);
        }
        return self;
    }

    /**
     * Sets the without label parameter.
     * @param key
     * @return
     */
    public T withoutLabel(String key) {
        if (!hasParameter(KubernetesMessageHeaders.LABEL)) {
            getParameters().put(KubernetesMessageHeaders.LABEL, "!" + key);
        } else {
            getParameters().put(KubernetesMessageHeaders.LABEL, getParameters().get(KubernetesMessageHeaders.LABEL) + ",!" + key);
        }
        return self;
    }
}
