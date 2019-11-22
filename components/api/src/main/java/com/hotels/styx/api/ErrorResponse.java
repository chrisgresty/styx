/*
  Copyright (C) 2013-2019 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.api;

/**
 * Represents the body of an HTTP error response in a standard format.
 */
public class ErrorResponse {

    private String errorMessage;

    /**
     * Creates an {@link ErrorResponse} with an error message string.
     * @param errorMessage the error message
     */
    public ErrorResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Return the error message.
     * @return the error message.
     */
    public String errorMessage() {
        return errorMessage;
    }

}