/*
 * Copyright 2018 - 2023 Anton Tananaev (anton@traccar.org)
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
 */
package com.trackolet.trackolet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.velocity.app.VelocityEngine;
import com.trackolet.trackolet.broadcast.BroadcastService;
import com.trackolet.trackolet.broadcast.MulticastBroadcastService;
import com.trackolet.trackolet.broadcast.RedisBroadcastService;
import com.trackolet.trackolet.broadcast.NullBroadcastService;
import com.trackolet.trackolet.config.Config;
import com.trackolet.trackolet.config.Keys;
import com.trackolet.trackolet.database.LdapProvider;
import com.trackolet.trackolet.database.OpenIdProvider;
import com.trackolet.trackolet.database.StatisticsManager;
import com.trackolet.trackolet.forward.EventForwarder;
import com.trackolet.trackolet.forward.EventForwarderJson;
import com.trackolet.trackolet.forward.EventForwarderAmqp;
import com.trackolet.trackolet.forward.EventForwarderKafka;
import com.trackolet.trackolet.forward.EventForwarderMqtt;
import com.trackolet.trackolet.forward.PositionForwarder;
import com.trackolet.trackolet.forward.PositionForwarderJson;
import com.trackolet.trackolet.forward.PositionForwarderAmqp;
import com.trackolet.trackolet.forward.PositionForwarderKafka;
import com.trackolet.trackolet.forward.PositionForwarderRedis;
import com.trackolet.trackolet.forward.PositionForwarderUrl;
import com.trackolet.trackolet.geocoder.AddressFormat;
import com.trackolet.trackolet.geocoder.BanGeocoder;
import com.trackolet.trackolet.geocoder.BingMapsGeocoder;
import com.trackolet.trackolet.geocoder.FactualGeocoder;
import com.trackolet.trackolet.geocoder.GeoapifyGeocoder;
import com.trackolet.trackolet.geocoder.GeocodeFarmGeocoder;
import com.trackolet.trackolet.geocoder.GeocodeXyzGeocoder;
import com.trackolet.trackolet.geocoder.Geocoder;
import com.trackolet.trackolet.geocoder.GisgraphyGeocoder;
import com.trackolet.trackolet.geocoder.GoogleGeocoder;
import com.trackolet.trackolet.geocoder.HereGeocoder;
import com.trackolet.trackolet.geocoder.LocationIqGeocoder;
import com.trackolet.trackolet.geocoder.MapQuestGeocoder;
import com.trackolet.trackolet.geocoder.MapTilerGeocoder;
import com.trackolet.trackolet.geocoder.MapboxGeocoder;
import com.trackolet.trackolet.geocoder.MapmyIndiaGeocoder;
import com.trackolet.trackolet.geocoder.NominatimGeocoder;
import com.trackolet.trackolet.geocoder.OpenCageGeocoder;
import com.trackolet.trackolet.geocoder.PositionStackGeocoder;
import com.trackolet.trackolet.geocoder.TestGeocoder;
import com.trackolet.trackolet.geocoder.TomTomGeocoder;
import com.trackolet.trackolet.geolocation.GeolocationProvider;
import com.trackolet.trackolet.geolocation.GoogleGeolocationProvider;
import com.trackolet.trackolet.geolocation.MozillaGeolocationProvider;
import com.trackolet.trackolet.geolocation.OpenCellIdGeolocationProvider;
import com.trackolet.trackolet.geolocation.UnwiredGeolocationProvider;
import com.trackolet.trackolet.handler.GeocoderHandler;
import com.trackolet.trackolet.handler.GeolocationHandler;
import com.trackolet.trackolet.handler.SpeedLimitHandler;
import com.trackolet.trackolet.helper.ObjectMapperContextResolver;
import com.trackolet.trackolet.helper.SanitizerModule;
import com.trackolet.trackolet.helper.WebHelper;
import com.trackolet.trackolet.mail.LogMailManager;
import com.trackolet.trackolet.mail.MailManager;
import com.trackolet.trackolet.mail.SmtpMailManager;
import com.trackolet.trackolet.session.cache.CacheManager;
import com.trackolet.trackolet.sms.HttpSmsClient;
import com.trackolet.trackolet.sms.SmsManager;
import com.trackolet.trackolet.sms.SnsSmsClient;
import com.trackolet.trackolet.speedlimit.OverpassSpeedLimitProvider;
import com.trackolet.trackolet.speedlimit.SpeedLimitProvider;
import com.trackolet.trackolet.storage.DatabaseStorage;
import com.trackolet.trackolet.storage.MemoryStorage;
import com.trackolet.trackolet.storage.Storage;
import com.trackolet.trackolet.web.WebServer;
import com.trackolet.trackolet.api.security.LoginService;

import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Properties;

public class MainModule extends AbstractModule {

    private final String configFile;

    public MainModule(String configFile) {
        this.configFile = configFile;
    }

    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named("configFile")).to(configFile);
        bind(Config.class).asEagerSingleton();
        bind(Timer.class).to(HashedWheelTimer.class).in(Scopes.SINGLETON);
    }

    @Singleton
    @Provides
    public static Storage provideStorage(Injector injector, Config config) {
        if (config.getBoolean(Keys.DATABASE_MEMORY)) {
            return injector.getInstance(MemoryStorage.class);
        } else {
            return injector.getInstance(DatabaseStorage.class);
        }
    }

    @Singleton
    @Provides
    public static ObjectMapper provideObjectMapper(Config config) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (config.getBoolean(Keys.WEB_SANITIZE)) {
            objectMapper.registerModule(new SanitizerModule());
        }
        objectMapper.registerModule(new JSONPModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Singleton
    @Provides
    public static Client provideClient(ObjectMapperContextResolver objectMapperContextResolver) {
        return ClientBuilder.newClient().register(objectMapperContextResolver);
    }

    @Singleton
    @Provides
    public static SmsManager provideSmsManager(Config config, Client client) {
        if (config.hasKey(Keys.SMS_HTTP_URL)) {
            return new HttpSmsClient(config, client);
        } else if (config.hasKey(Keys.SMS_AWS_REGION)) {
            return new SnsSmsClient(config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static MailManager provideMailManager(Config config, StatisticsManager statisticsManager) {
        if (config.getBoolean(Keys.MAIL_DEBUG)) {
            return new LogMailManager();
        } else {
            return new SmtpMailManager(config, statisticsManager);
        }
    }

    @Singleton
    @Provides
    public static LdapProvider provideLdapProvider(Config config) {
        if (config.hasKey(Keys.LDAP_URL)) {
            return new LdapProvider(config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static OpenIdProvider provideOpenIDProvider(
        Config config, LoginService loginService, ObjectMapper objectMapper
        ) throws InterruptedException, IOException, URISyntaxException {
        if (config.hasKey(Keys.OPENID_CLIENT_ID)) {
            return new OpenIdProvider(config, loginService, HttpClient.newHttpClient(), objectMapper);
        }
        return null;
    }

    @Provides
    public static WebServer provideWebServer(Injector injector, Config config) {
        if (config.hasKey(Keys.WEB_PORT)) {
            return new WebServer(injector, config);
        }
        return null;
    }

    @Singleton
    @Provides
    public static Geocoder provideGeocoder(Config config, Client client, StatisticsManager statisticsManager) {
        if (config.getBoolean(Keys.GEOCODER_ENABLE)) {
            String type = config.getString(Keys.GEOCODER_TYPE.getKey(), "google");
            String url = config.getString(Keys.GEOCODER_URL);
            String key = config.getString(Keys.GEOCODER_KEY);
            String language = config.getString(Keys.GEOCODER_LANGUAGE);
            String formatString = config.getString(Keys.GEOCODER_FORMAT);
            AddressFormat addressFormat = formatString != null ? new AddressFormat(formatString) : new AddressFormat();

            int cacheSize = config.getInteger(Keys.GEOCODER_CACHE_SIZE);
            Geocoder geocoder;
            switch (type) {
                case "test":
                    geocoder = new TestGeocoder();
                    break;
                case "nominatim":
                    geocoder = new NominatimGeocoder(client, url, key, language, cacheSize, addressFormat);
                    break;
                case "locationiq":
                    geocoder = new LocationIqGeocoder(client, url, key, language, cacheSize, addressFormat);
                    break;
                case "gisgraphy":
                    geocoder = new GisgraphyGeocoder(client, url, cacheSize, addressFormat);
                    break;
                case "mapquest":
                    geocoder = new MapQuestGeocoder(client, url, key, cacheSize, addressFormat);
                    break;
                case "opencage":
                    geocoder = new OpenCageGeocoder(client, url, key, language, cacheSize, addressFormat);
                    break;
                case "bingmaps":
                    geocoder = new BingMapsGeocoder(client, url, key, cacheSize, addressFormat);
                    break;
                case "factual":
                    geocoder = new FactualGeocoder(client, url, key, cacheSize, addressFormat);
                    break;
                case "geocodefarm":
                    geocoder = new GeocodeFarmGeocoder(client, key, language, cacheSize, addressFormat);
                    break;
                case "geocodexyz":
                    geocoder = new GeocodeXyzGeocoder(client, key, cacheSize, addressFormat);
                    break;
                case "ban":
                    geocoder = new BanGeocoder(client, cacheSize, addressFormat);
                    break;
                case "here":
                    geocoder = new HereGeocoder(client, url, key, language, cacheSize, addressFormat);
                    break;
                case "mapmyindia":
                    geocoder = new MapmyIndiaGeocoder(client, url, key, cacheSize, addressFormat);
                    break;
                case "tomtom":
                    geocoder = new TomTomGeocoder(client, url, key, cacheSize, addressFormat);
                    break;
                case "positionstack":
                    geocoder = new PositionStackGeocoder(client, key, cacheSize, addressFormat);
                    break;
                case "mapbox":
                    geocoder = new MapboxGeocoder(client, key, cacheSize, addressFormat);
                    break;
                case "maptiler":
                    geocoder = new MapTilerGeocoder(client, key, cacheSize, addressFormat);
                    break;
                case "geoapify":
                    geocoder = new GeoapifyGeocoder(client, key, language, cacheSize, addressFormat);
                    break;
                default:
                    geocoder = new GoogleGeocoder(client, key, language, cacheSize, addressFormat);
                    break;
            }
            geocoder.setStatisticsManager(statisticsManager);
            return geocoder;
        }
        return null;
    }

    @Singleton
    @Provides
    public static GeolocationProvider provideGeolocationProvider(Config config, Client client) {
        if (config.getBoolean(Keys.GEOLOCATION_ENABLE)) {
            String type = config.getString(Keys.GEOLOCATION_TYPE.getKey(), "mozilla");
            String url = config.getString(Keys.GEOLOCATION_URL);
            String key = config.getString(Keys.GEOLOCATION_KEY);
            switch (type) {
                case "google":
                    return new GoogleGeolocationProvider(client, key);
                case "opencellid":
                    return new OpenCellIdGeolocationProvider(client, url, key);
                case "unwired":
                    return new UnwiredGeolocationProvider(client, url, key);
                default:
                    return new MozillaGeolocationProvider(client, key);
            }
        }
        return null;
    }

    @Singleton
    @Provides
    public static SpeedLimitProvider provideSpeedLimitProvider(Config config, Client client) {
        if (config.getBoolean(Keys.SPEED_LIMIT_ENABLE)) {
            String type = config.getString(Keys.SPEED_LIMIT_TYPE.getKey(), "overpass");
            String url = config.getString(Keys.SPEED_LIMIT_URL);
            switch (type) {
                case "overpass":
                default:
                    return new OverpassSpeedLimitProvider(config, client, url);
            }
        }
        return null;
    }

    @Singleton
    @Provides
    public static GeolocationHandler provideGeolocationHandler(
            Config config, @Nullable GeolocationProvider geolocationProvider, CacheManager cacheManager,
            StatisticsManager statisticsManager) {
        if (geolocationProvider != null) {
            return new GeolocationHandler(config, geolocationProvider, cacheManager, statisticsManager);
        }
        return null;
    }

    @Singleton
    @Provides
    public static GeocoderHandler provideGeocoderHandler(
            Config config, @Nullable Geocoder geocoder, CacheManager cacheManager) {
        if (geocoder != null) {
            return new GeocoderHandler(config, geocoder, cacheManager);
        }
        return null;
    }

    @Singleton
    @Provides
    public static SpeedLimitHandler provideSpeedLimitHandler(@Nullable SpeedLimitProvider speedLimitProvider) {
        if (speedLimitProvider != null) {
            return new SpeedLimitHandler(speedLimitProvider);
        }
        return null;
    }

    @Singleton
    @Provides
    public static BroadcastService provideBroadcastService(
            Config config, ObjectMapper objectMapper) throws IOException {
        if (config.hasKey(Keys.BROADCAST_TYPE)) {
            switch (config.getString(Keys.BROADCAST_TYPE)) {
                case "multicast":
                    return new MulticastBroadcastService(config, objectMapper);
                case "redis":
                    return new RedisBroadcastService(config, objectMapper);
                default:
                    break;
            }
        }
        return new NullBroadcastService();
    }

    @Singleton
    @Provides
    public static EventForwarder provideEventForwarder(Config config, Client client, ObjectMapper objectMapper) {
        if (config.hasKey(Keys.EVENT_FORWARD_URL)) {
            String forwardType = config.getString(Keys.EVENT_FORWARD_TYPE);
            switch (forwardType) {
                case "amqp":
                    return new EventForwarderAmqp(config, objectMapper);
                case "kafka":
                    return new EventForwarderKafka(config, objectMapper);
                case "mqtt":
                    return new EventForwarderMqtt(config, objectMapper);
                case "json":
                default:
                    return new EventForwarderJson(config, client);
            }
        }
        return null;
    }

    @Singleton
    @Provides
    public static PositionForwarder providePositionForwarder(Config config, Client client, ObjectMapper objectMapper) {
        if (config.hasKey(Keys.FORWARD_URL)) {
            switch (config.getString(Keys.FORWARD_TYPE)) {
                case "json":
                    return new PositionForwarderJson(config, client, objectMapper);
                case "amqp":
                    return new PositionForwarderAmqp(config, objectMapper);
                case "kafka":
                    return new PositionForwarderKafka(config, objectMapper);
                case "redis":
                    return new PositionForwarderRedis(config, objectMapper);
                case "url":
                default:
                    return new PositionForwarderUrl(config, client, objectMapper);
            }
        }
        return null;
    }

    @Singleton
    @Provides
    public static VelocityEngine provideVelocityEngine(Config config) {
        Properties properties = new Properties();
        properties.setProperty("resource.loader.file.path", config.getString(Keys.TEMPLATES_ROOT) + "/");
        properties.setProperty("web.url", WebHelper.retrieveWebUrl(config));

        VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.init(properties);
        return velocityEngine;
    }

}
