/*
 * Copyright 2015 Adam Peck.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bencoding;

/**
 * Validates non-String types.
 *
 * @author Adam Peck
 */
final class TypeValidator implements Validator {

    private final char type;

    public TypeValidator(final char type) {
        this.type = type;
    }

    public boolean validate(final int token) {
        return token == type;
    }
}
