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
package org.srcdeps.core.fs;

/**
 * Thrown when a lock cannot be acquired.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CannotAcquireLockException extends Exception {

    private static final long serialVersionUID = 7292006204712515755L;

    public CannotAcquireLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public CannotAcquireLockException(String message) {
        super(message);
    }

    public CannotAcquireLockException(Throwable cause) {
        super(cause);
    }

}
