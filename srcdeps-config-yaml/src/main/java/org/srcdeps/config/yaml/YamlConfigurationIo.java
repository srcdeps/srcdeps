/**
 * Copyright 2015-2016 Maven Source Dependencies
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
package org.srcdeps.config.yaml;

import java.io.Reader;

import org.srcdeps.config.yaml.internal.SrcdepsConstructor;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ConfigurationException;
import org.srcdeps.core.config.ConfigurationIo;
import org.yaml.snakeyaml.Yaml;

/**
 * Reads {@link Configuration} from a YAML file.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class YamlConfigurationIo implements ConfigurationIo {

    @Override
    public Configuration read(Reader in) throws ConfigurationException {
        Yaml yaml = new Yaml(new SrcdepsConstructor());
        Configuration.Builder builder = yaml.loadAs(in, Configuration.Builder.class);
        return builder.build();

    }

}
