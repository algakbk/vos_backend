/**
 * "First, solve the problem. Then, write the code. -John Johnson"
 * "Or use Vangav M"
 * www.vangav.com
 * */

/**
 * MIT License
 *
 * Copyright (c) 2016 Vangav
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * */

/**
 * Community
 * Facebook Group: Vangav Open Source - Backend
 *   fb.com/groups/575834775932682/
 * Facebook Page: Vangav
 *   fb.com/vangav.f
 * 
 * Third party communities for Vangav Backend
 *   - play framework
 *   - cassandra
 *   - datastax
 *   
 * Tag your question online (e.g.: stack overflow, etc ...) with
 *   #vangav_backend
 *   to easier find questions/answers online
 * */

package com.vangav.backend.networks.rest_client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author mustapha
 * fb.com/mustapha.abdallah
 */
/**
 * RestResponseJson is the parent class for a REST response's JSON
 * */
public abstract class RestResponseJson {
  
  /**
   * getName
   * @return the name of the child class inheriting from this class
   * @throws Exception
   */
  @JsonIgnore
  protected abstract String getName () throws Exception;
  
  /**
   * getThis
   * @return the child instance inheriting from this class
   * @throws Exception
   */
  @JsonIgnore
  protected abstract RestResponseJson getThis () throws Exception;
  
  /**
   * fromJsonString
   * @param json representation of a REST's response
   * @return  json object representation of the response
   * @throws Exception
   */
  @JsonIgnore
  final public RestResponseJson fromJsonString (
    String json) throws Exception {
    
    ObjectMapper objectMapper = new ObjectMapper();
    
    return
      (RestResponseJson)
      (objectMapper.readValue(json, this.getThis().getClass() ) );
  }
  
  /**
   * getAsString
   * @return string representation of the REST response JSON
   * @throws Exception
   */
  @JsonIgnore
  final public String getAsString () throws Exception {
    
    ObjectMapper objectMapper = new ObjectMapper();
    
    return
      objectMapper.writerWithDefaultPrettyPrinter(
        ).writeValueAsString(this.getThis() );
  }
  
  @Override
  @JsonIgnore
  public String toString () {
    
    try {
    
      return
        "REST Response JSON ["
        + this.getName()
        + "]:\n"
        + this.getAsString();
    } catch (Exception e) {
      
      return
        "REST Response JSON: threw an Exception!";
    }
  }
}
