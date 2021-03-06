
### [![YouTube Play Icon](http://youtube.com/favicon.ico)](https://www.youtube.com/watch?v=gDrcH1bopfU&index=4&list=PLTcKayTjao6rOj02gtRdiVhvzB1SWGyhv&t=406) **on [YouTube](https://www.youtube.com/watch?v=gDrcH1bopfU&index=4&list=PLTcKayTjao6rOj02gtRdiVhvzB1SWGyhv&t=406)**

> **why?** sooner or later you will need to generate something that needs a database and/or utilizes some of vangav backend's built-in utilities - the geo services utility is picked for this tutorial to show how something that's a bit complicated like reverse geo coding is compressed to a one method call in vangav backend

# [geo server](https://github.com/vangav/vos_geo_server)

> 15-20 min: this tutorial explains how to generate a next level service (compared to calculate sum); including database, using utilities and data

geo server is a service that takes a latitude/longitude request and returns the hash code and reverse geo code (continent, country, major city, city); it also keeps track of queried continents/countries to provide lists of sorted top queried continents and countries

![backend design](https://scontent-mad1-1.xx.fbcdn.net/v/t31.0-8/21056159_1976185445930432_4615169054130938272_o.png?oh=c1c3350588525405d937cc20bf552e88&oe=5A19D864)

### init
> skip this section if you already did it in [init](https://github.com/vangav/vos_backend#init) of the calculate sum example
1. create a workspace directory `my_services` - this is the directory to contain both of vos_backend and all the services generated using it
2. download this `vos_backend.zip` project (from the green `clone or download` button up there) inside the workspace directory created in (1) and unzip it
3. **rename** downloaded vos_backend-master to vos_backend

### generate a new service

1. create a new directory `my_services/vos_geo_server`
2. copy `controllers.json` and `gs_top.keyspace` from `vos_backend/vangav_backend_templates/vos_geo_server/` to the directory `my_services/vos_geo_server` created in (1)
3. open a terminal session and `cd` to `my_services/vos_backend/tools_bin`
4. execute the command `java -jar backend_generator.jar new vos_geo_server` to generate the service
5. enter `y` for using the config directory in order to use `controllers.json` and `gs_top.keyspace` for generating
6. enter `n` for generating a worker service (using workers is explained in a separate section)

### init the service's cassandra database
1. `cd` to `my_services/vos_geo_server/cassandra/cql/`
2. execute the command `./_start_cassandra.sh` to start cassandra
3. `cd` to `my_services/vos_geo_server/cassandra/cql/drop_and_create/`
4. execute the command `./_execute_cql.sh gs_top_dev.cql` to initialize the service's database tables

### init service's data
1. `copy` the contents of the directory `my_services/vos_backend/data/geo/reverse_geo_coding/` to `my_services/vos_geo_server/conf/data/geo/reverse_geo_coding/`

### start the service
1. `cd` to `my_services/vos_geo_server`
2. execute the command `./_run.sh`

### preliminary testing
1. open an internet browser page and type [`http://localhost:9000/reverse_geo_code?latitude=49&longitude=11`](http://localhost:9000/reverse_geo_code?latitude=49&longitude=11), this returns an empty response

### stop the service
1. in the terminal session where you started the service press `control + d`

### writing the service's logic code
+ optionally for eclipse users: open eclipse and import vos_geo_server project
  + file **>** import **>** general **>** existing projects into workspace **>** next **>** set "select root directory" to my_services **>** under projects make sure that vos_geo_server is selected **>** finish
  + double check the java version used for compiling the project: right click the project **>** properties **>** java compiler **>** enable project specific settings **>** compiler compliance level **>** 1.7 or 1.8
#### - index initialization
+ under package `com.vangav.vos_geo_server` add a new package [`common`](https://github.com/vangav/vos_geo_server/tree/master/app/com/vangav/vos_geo_server/common)
+ in the created package in the previous step add a new class [InitIndexInl.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/common/InitIndexInl.java) with the following implementation:
```java
package com.vangav.vos_geo_server.common;

import com.datastax.driver.core.ResultSet;
import com.vangav.vos_geo_server.cassandra_keyspaces.gs_top.NameIndex;

/**
 * InitIndexInl has an inline static method to init Cassandra's gs_top.index
 *   table by inserting index_key values (continents and countries)
 * */
public class InitIndexInl {

  public static final String kContinentsIndexKey = "continents";
  public static final String kCountriesIndexKey = "countries";
  /**
   * initIndex
   * does first-run initialization for gs_top.index table
   * @throws Exception
   */
  public static void initIndex () throws Exception {
    
    ResultSet resultSet = NameIndex.i().executeSyncSelect(kContinentsIndexKey);
    
    if (resultSet.isExhausted() == true) {
      
      NameIndex.i().executeSyncInsert(kContinentsIndexKey);
    }
    
    resultSet = NameIndex.i().executeSyncSelect(kCountriesIndexKey);
    
    if (resultSet.isExhausted() == true) {
      
      NameIndex.i().executeSyncInsert(kCountriesIndexKey);
    }
  }
}
```
#### - load reverse geo coding data and initialize index on service start
+ in [default_package/Global.java](https://github.com/vangav/vos_geo_server/blob/master/app/Global.java) after the [following line](https://github.com/vangav/vos_geo_server/blob/master/app/Global.java#L94)
```java
Countries.loadTable();
```
add the [following lines](https://github.com/vangav/vos_geo_server/blob/master/app/Global.java#L98)
```java
ReverseGeoCoding.load();
InitIndexInl.initIndex();
```
#### - processing reverse geo coding
+ open class [HandlerReverseGeoCode.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/reverse_geo_code/HandlerReverseGeoCode.java), method [`processRequest`](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/reverse_geo_code/HandlerReverseGeoCode.java#L96) should be as follows
```java
  @Override
  protected void processRequest (final Request request) throws Exception {

    // use the following request Object to process the request and set
    //   the response to be returned
    RequestReverseGeoCode requestReverseGeoCode =
      (RequestReverseGeoCode)request.getRequestJsonBody();
    
    // get geo hash
    String geoHash =
      GeoHash.geoHashStringWithCharacterPrecision(
        requestReverseGeoCode.latitude,
        requestReverseGeoCode.longitude,
        12);
    
    // get reverse geo code
    ReverseGeoCode reverseGeoCode =
      ReverseGeoCoding.i().getReverseGeoCode(
        requestReverseGeoCode.latitude,
        requestReverseGeoCode.longitude);
    
    // set response
    ((ResponseReverseGeoCode)request.getResponseBody() ).set(
      requestReverseGeoCode.latitude,
      requestReverseGeoCode.longitude,
      geoHash,
      reverseGeoCode.getCity(),
      reverseGeoCode.getMajorCity(),
      reverseGeoCode.getCountryCode(),
      reverseGeoCode.getCountry(),
      reverseGeoCode.getContinentCode(),
      reverseGeoCode.getContinent() );
  }
```
#### - update top queried locations data in after-processing
+ then add the following [`method`](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/reverse_geo_code/HandlerReverseGeoCode.java#L130) in class [HandlerReverseGeoCode.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/reverse_geo_code/HandlerReverseGeoCode.java)
```java
  @Override
  protected void afterProcessing (
    final Request request) throws Exception {

    // get request Object
    RequestReverseGeoCode requestReverseGeoCode =
      (RequestReverseGeoCode)request.getRequestJsonBody();
    
    // get reverse geo code
    ReverseGeoCode reverseGeoCode =
      ReverseGeoCoding.i().getReverseGeoCode(
        requestReverseGeoCode.latitude,
        requestReverseGeoCode.longitude);
    
    // update continents index
    NameIndex.i().executeAsyncUpdate(
      new HashSet<String>(Arrays.asList(reverseGeoCode.getContinent() ) ),
      InitIndexInl.kContinentsIndexKey);
    
    // update countries index
    NameIndex.i().executeAsyncUpdate(
      new HashSet<String>(Arrays.asList(reverseGeoCode.getCountry() ) ),
      InitIndexInl.kCountriesIndexKey);
    
    // update continents counter's value
    Continents.i().executeAsyncUpdateCounterValue(
      reverseGeoCode.getContinent() );
    
    // update countries counter's value
    Countries.i().executeAsyncUpdateCounterValue(
      reverseGeoCode.getCountry() );
  }
```
#### - complete the top queried continents' response structure (nested json)
+ under package [com.vangav.vos_geo_server.controllers.top_continents](https://github.com/vangav/vos_geo_server/tree/master/app/com/vangav/vos_geo_server/controllers/top_continents) add class [ResponseTopContinent.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_continents/response_json/ResponseTopContinent.java) with the following code
```java
package com.vangav.vos_geo_server.controllers.top_continents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ResponseTopContinent represents the response's top-continent
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseTopContinent {
  
  /**
   * Constructor ResponseTopContinent
   * @param continentName
   * @param continentCount
   * @return new ResponseTopContinent Object
   * @throws Exception
   */
  @JsonIgnore
  public ResponseTopContinent (
    String continentName,
    long continentCount) throws Exception {
    
    this.continent_name = continentName;
    this.continent_count = continentCount;
  }

  @JsonProperty
  public String continent_name;
  @JsonProperty
  public long continent_count;
}
```
+ modify class [ResponseTopContinents.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_continents/ResponseTopContinents.java) to be as follows:
```java
package com.vangav.vos_geo_server.controllers.top_continents;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vangav.backend.play_framework.request.response.ResponseBodyJson;

/**
 * ResponseTopContinents represents the response's structure
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseTopContinents extends ResponseBodyJson {

  @Override
  @JsonIgnore
  protected String getName () throws Exception {

    return "TopContinents";
  }

  @Override
  @JsonIgnore
  protected ResponseTopContinents getThis () throws Exception {

    return this;
  }

  @JsonProperty
  public ResponseTopContinent[] top_continents;
  
  @JsonIgnore
  public void set (ResponseTopContinent[] top_continents) {
    
    this.top_continents = top_continents;
  }
}
```
#### - processing top queried continents
+ in class [HandlerTopContinents.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_continents/HandlerTopContinents.java) method [`processRequest`](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_continents/HandlerTopContinents.java#L97) should be as follows:
```java
  @Override
  protected void processRequest (final Request request) throws Exception {
    
    // select continents from gs_top.index
    ResultSet resultSet =
      NameIndex.i().executeSyncSelect(InitIndexInl.kContinentsIndexKey);
    
    // no continents queried before?
    if (resultSet.isExhausted() == true) {
      
      ((ResponseTopContinents)request.getResponseBody() ).set(
        new ResponseTopContinent[0] );
      
      return;
    }
    
    // extract continents index
    Set<String> continentsIndex =
      resultSet.one().getSet(
        NameIndex.kIndexValuesColumnName,
        String.class);
    
    // init top continents
    ArrayList<Pair<String, Long> > topContinents =
      new ArrayList<Pair<String, Long> >();
    
    // for each continent
    for (String continent : continentsIndex) {
      
      // select continent's counter-value
      resultSet = Continents.i().executeSyncSelectCounterValue(continent);
      
      // no data? skip
      if (resultSet.isExhausted() == true) {
        
        continue;
      }
      
      // store continent name-counter pair
      topContinents.add(
        new Pair<String, Long>(
          continent,
          resultSet.one().getLong(Continents.kCounterValueColumnName) ) );
    }
    
    // sort results ascending
    Collections.sort(topContinents, new Comparator<Pair<String, Long> > () {

      @Override
      public int compare (Pair<String, Long> x, Pair<String, Long> y) {

        return Long.compare(x.getSecond(), y.getSecond() );
      }
    } );
    
    // reverse sorted results to put them in a descending order
    Collections.reverse(topContinents);
    
    // fill response array
    
    ResponseTopContinent[] responseArray =
      new ResponseTopContinent[topContinents.size() ];
    
    for (int i = 0; i < topContinents.size(); i ++) {
      
      responseArray[i] =
        new ResponseTopContinent(
          topContinents.get(i).getFirst(),
          topContinents.get(i).getSecond() );
    }
    
    // set response
    ((ResponseTopContinents)request.getResponseBody() ).set(responseArray);
  }
```
+ now repeat the last 3 steps for countries to add [ResponseTopCountry.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_countries/response_json/ResponseTopCountry.java) then update [ResponseTopCountries.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_countries/ResponseTopCountries.java) and [HandlerTopCountries.java](https://github.com/vangav/vos_geo_server/blob/master/app/com/vangav/vos_geo_server/controllers/top_countries/HandlerTopCountries.java), for reference check the [finished version](https://github.com/vangav/vos_geo_server)

### start the service
1. `cd` to `my_services/vos_geo_server`
2. execute the command `./_run.sh`

### try it out
1. open an internet browser page and type any of
  + [`http://localhost:9000/reverse_geo_code?latitude=49&longitude=11`](http://localhost:9000/reverse_geo_code?latitude=49&longitude=11) - play around with the latitude and longitude values
  + [`http://localhost:9000/top_continents`](http://localhost:9000/top_continents)
  + [`http://localhost:9000/top_countries`](http://localhost:9000/top_countries)

### stop the service
1. in the terminal session where you started the service press `control + d`

### how to stop cassandra
1. in a terminal session execute `ps auwx | grep  cassandra`; this shows cassandra's `pid` (process id)
2. execute `kill -9 (pid)` (pid) got from step (1)
3. repeat step (1) to make sure you get one (pid) only - that's the pid for the `grep command`

# exercise
> add a feature that gets the top queried continents/countries by month; here are some tips ;-)
+ in [gs_top.keyspace](https://github.com/vangav/vos_geo_server/blob/master/generator_config/gs_top.keyspace) add two table `monthly_continents` and `monthly_countries` with key columns `month_continent` and `month_country` respectively
+ use [cassandra_updater](https://github.com/vangav/vos_geo_server/tree/master/cassandra_updater) as explained in this [tutorial](https://github.com/vangav/vos_backend/blob/master/README/03_generated_rest_service_structure.md#cassandra_updater) to update the java clients and cql scripts
+ add two new controllers as explained in this [tutorial](https://github.com/vangav/vos_backend/blob/master/README/00_expanding_calculate_sum_example.md#expand-calculate-sum-to-calculator-without-regenerating-the-service); the two new controllers are similar to [top_continents](https://github.com/vangav/vos_geo_server/tree/master/app/com/vangav/vos_geo_server/controllers/top_continents) and [top_countries](https://github.com/vangav/vos_geo_server/tree/master/app/com/vangav/vos_geo_server/controllers/top_countries) but take a month param in their requests

# next tutorial -> [generated service structure](https://github.com/vangav/vos_backend/blob/master/README/03_generated_rest_service_structure.md)
> explains the building blocks of a generated service

# share

[![facebook share](https://www.prekindle.com/images/social/facebook.png)](https://www.facebook.com/sharer/sharer.php?u=https%3A//github.com/vangav/vos_backend)  [![twitter share](http://www.howickbaptist.org.nz/wordpress/media/twitter-64-black.png)](https://twitter.com/home?status=vangav%20backend%20%7C%20build%20big%20tech%2010x%20faster%20%7C%20https%3A//github.com/vangav/vos_backend)  [![pinterest share](http://d7ab823tjbf2qywyt3grgq63.wpengine.netdna-cdn.com/wp-content/themes/velominati/images/share_icons/pinterest-black.png)](https://pinterest.com/pin/create/button/?url=https%3A//github.com/vangav/vos_backend&media=https%3A//scontent-mad1-1.xx.fbcdn.net/v/t31.0-8/20645143_1969408006608176_5289565717021239224_o.png?oh=acf20113a3673409d238924cfec648d2%26oe=5A3414B5&description=)  [![google plus share](http://e-airllc.com/wp-content/themes/nebula/images/social_black/google.png)](https://plus.google.com/share?url=https%3A//github.com/vangav/vos_backend)  [![linkedin share](http://e-airllc.com/wp-content/themes/nebula/images/social_black/linkedin.png)](https://www.linkedin.com/shareArticle?mini=true&url=https%3A//github.com/vangav/vos_backend&title=vangav%20backend%20%7C%20build%20big%20tech%2010x%20faster&summary=&source=)

# free consulting

[![vangav's consultant](http://www.footballhighlights247.com/images/mobile-share/fb-messenger-64x64.png)](https://www.facebook.com/mustapha.abdallah)
