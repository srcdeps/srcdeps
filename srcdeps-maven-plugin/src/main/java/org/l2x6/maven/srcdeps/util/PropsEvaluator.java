/**
 * Copyright 2015 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.maven.srcdeps.util;

import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;

public class PropsEvaluator {
    private final PluginParameterExpressionEvaluator evaluator;

    public PropsEvaluator(PluginParameterExpressionEvaluator evaluator) {
        super();
        this.evaluator = evaluator;
    }

    public Optional<String> stringOptional(final Element element) {
        return stringOptional(element.toSrcDepsPropertyExpression());
    }

    public Optional<String> stringOptional(final String expression) {
        try {
            return Optional.ofNullable((String) evaluator.evaluate(expression));
        } catch (ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }

    public Supplier<String> stringSupplier(final Element element) {
        return stringSupplier(element.toSrcDepsPropertyExpression());
    }

    public Supplier<String> stringSupplier(final String expression) {
        return new Supplier<String>() {
            @Override
            public String get() {
                try {
                    return (String) evaluator.evaluate(expression);
                } catch (ExpressionEvaluationException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}