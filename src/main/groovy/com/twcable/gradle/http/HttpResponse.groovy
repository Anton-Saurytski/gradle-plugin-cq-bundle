/*
 * Copyright 2014-2017 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twcable.gradle.http

import groovy.json.JsonSlurper

class HttpResponse {
    final int code
    final String body
    private final JsonSlurper jsonSlurper = new JsonSlurper()


    HttpResponse(int code, String body) {
        this.code = code
        this.body = body
    }


    def asType(Class type) {
        switch (type) {
            case String: this.body; break
            case Number: this.code; break
            case Map: jsonSlurper.parseText(this.body); break
            default: throw new ClassCastException("Can not cast ${this.class} as ${type}")
        }
    }


    @Override
    public String toString() {
        return "[${code}, \"${body}\"]"
    }


    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        HttpResponse that = (HttpResponse)o

        if (code != that.code) return false
        if (body != that.body) return false

        return true
    }


    int hashCode() {
        int result
        result = code
        result = 31 * result + (body != null ? body.hashCode() : 0)
        return result
    }
}
