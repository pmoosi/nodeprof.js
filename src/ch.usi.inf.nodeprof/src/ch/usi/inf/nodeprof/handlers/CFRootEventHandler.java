/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
 * *****************************************************************************/
package ch.usi.inf.nodeprof.handlers;

import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;

import ch.usi.inf.nodeprof.ProfiledTagEnum;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.objects.Undefined;

public abstract class CFRootEventHandler extends BaseSingleTagEventHandler {

    private final String type;

    public CFRootEventHandler(EventContext context) {
        super(context, ProfiledTagEnum.CF_ROOT);
        this.type = getAttributeInternalString("type");
    }

    public TruffleString getType() {
        // XXX cache string
        return Strings.fromJavaString(type);
    }

    public Object getCondition(Object[] inputs) {
        Object cond = inputs != null && inputs.length > 0 ? inputs[0] : Undefined.instance;
        return cond != null ? cond : Undefined.instance;
    }

    public boolean isAsyncRoot() {
        return this.type.equals(JSTags.ControlFlowRootTag.Type.AsyncFunction.name());
    }

    public boolean isForIn() {
        return type.equals(JSTags.ControlFlowRootTag.Type.ForInIteration.name());
    }

    public boolean isForOf() {
        return type.equals(JSTags.ControlFlowRootTag.Type.ForOfIteration.name());
    }
}
