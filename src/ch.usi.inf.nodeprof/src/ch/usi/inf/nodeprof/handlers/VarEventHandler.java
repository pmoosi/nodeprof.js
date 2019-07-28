/*******************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
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
 *******************************************************************************/
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.EventContext;

import ch.usi.inf.nodeprof.ProfiledTagEnum;

/**
 * Abstract event handler for variable writes
 */
public abstract class VarEventHandler extends BaseSingleTagEventHandler {
    private final String name;

    public VarEventHandler(EventContext context, ProfiledTagEnum tag) {
        super(context, tag);
        this.name = (String) getAttribute("name");
    }

    public String getName() {
        return this.name;
    }
}
