/*
 * Copyright 2002-2005 the original author or authors.
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

package org.anodyneos.servlet.multipart.support;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.anodyneos.servlet.multipart.MultipartFile;
import org.anodyneos.servlet.multipart.MultipartHttpServletRequest;
import org.anodyneos.servlet.multipart.commons.CommonsMultipartFile;

/**
 * Abstract base implementation of the MultipartHttpServletRequest interface.
 * Provides management of pre-generated MultipartFile instances.
 *
 * @author Juergen Hoeller
 * @since 06.10.2003
 */
public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper
    implements MultipartHttpServletRequest {

    private Map<String, CommonsMultipartFile> multipartFiles;

    /**
     * Wrap the given HttpServletRequest in a MultipartHttpServletRequest.
     * @param request the request to wrap
     */
    protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    /**
     * Set a Map with parameter names as keys and MultipartFile objects as values.
     * To be invoked by subclasses on initialization.
     */
    protected void setMultipartFiles(Map<String, CommonsMultipartFile> multipartFiles) {
        this.multipartFiles = Collections.unmodifiableMap(multipartFiles);
    }

    public Iterator<String> getFileNames() {
        return this.multipartFiles.keySet().iterator();
    }

    public MultipartFile getFile(String name) {
        return this.multipartFiles.get(name);
    }

    public Map<String, CommonsMultipartFile> getFileMap() {
        return this.multipartFiles;
    }

}
