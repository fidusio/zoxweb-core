package org.zoxweb.server.http;


import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.task.TaskUtil;
import org.zoxweb.server.util.GSONUtil;
import org.zoxweb.shared.http.*;
import org.zoxweb.shared.util.*;

public class APIIntegration
{

    public static void createXlogistXLoginEndPoint()
    {
        // configuring the base http message object
        // the URL https://api.xlogistx.io
        // the URI /login
        HTTPMessageConfigInterface hmci = HTTPMessageConfig.createAndInit("https://api.xlogistx.io", "/login", HTTPMethod.GET);
        hmci.setName("login");
        HTTPAPIEndPoint<NVGenericMap, NVGenericMap> loginEP = HTTPAPIManager.SINGLETON.buildEndPoint(hmci, null, null);
        loginEP.setDomain("api.xlogistx.io")
        .setRateController(new RateController(loginEP.toCanonicalID(), "500/s"))
        .setScheduler(TaskUtil.defaultTaskScheduler());
        HTTPAPIManager.SINGLETON.register(loginEP);
    }

    public static void main(String ...args)
    {
        try {

            HTTPNVGMBiEncoder.log.setEnabled(true);

            createXlogistXLoginEndPoint();
            HTTPNVGMBiEncoder encoder = null;
            DataDecoder<HTTPResponseData, NVGenericMap> decoder = null;
            ParamUtil.ParamMap params = ParamUtil.parse("=", args);
            HTTPMessageConfigInterface config = null;
            String domain = null;
            HTTPAPIEndPoint<NVGenericMap, NVGenericMap> userAPI = null;

            if (params.nameExists("config"))
            {




                NVGenericMap nvgm = GSONUtil.fromJSONDefault(IOUtil.inputStreamToString(params.stringValue("config")), NVGenericMap.class, true);
                userAPI = HTTPAPIManager.SINGLETON.buildEndPoint(nvgm);
//                System.out.println(nvgm);
//                config = nvgm.getValue("hmci_config");
//                System.out.println(config);
//                domain = nvgm.getValue("domain");
//                NVGenericMap encoderMeta = nvgm.lookup("data_encoder");
//                String metaType = encoderMeta.getValue("meta_type");
//                String json = encoderMeta.getValue("content");
//                encoder = HTTPAPIManager.SINGLETON.buildCodec(encoderMeta);//(HTTPNVGMBiEncoder) GSONUtil.fromJSONDefault(json, Class.forName(metaType));
//                decoder = HTTPAPIManager.SINGLETON.buildCodec(nvgm.lookup("data_decoder"));
//                NVGenericMap nvgmRateController = nvgm.lookup("rate_controller");
//                RateController rateController = null;
//                if (nvgmRateController != null)
//                {
//                    rateController = new RateController(nvgmRateController.getValue("name"), nvgmRateController.getValue("rate"));
//                }
//
//                System.out.println(""+nvgm.lookup("data_encoder.content").getClass());
//                HTTPAPIManager.SINGLETON.buildEndPoint(config, encoder, decoder)
//                        .setRateController(rateController)
//                        .setScheduler(TaskUtil.getDefaultTaskScheduler())
//                        .setDomain(domain);

            }
            else
            {
                String apiKeyID = params.stringValue("api-key-id");
                String apiKey = params.stringValue("api-key");
                String apiURL = params.stringValue("url");
                String apiURI = params.stringValue("uri");
                String httpMethod = params.stringValue("request");


                config = HTTPMessageConfig.createAndInit(apiURL, apiURI, httpMethod);
                config.setAccept(HTTPMediaType.APPLICATION_JSON);
                config.setContentType(HTTPMediaType.APPLICATION_JSON);
                config.getHeaders().add("revision", "2023-09-15");
                config.setAuthorization(new HTTPAuthorization(apiKeyID, apiKey));
                config.setName("klaviyo.profile");
                config.setDescription("Profile API configuration");
            }



//            System.out.println(GSONUtil.toJSONDefault(config, true ));
//            NVGenericMap contentConfig = new NVGenericMap();
//            NVGenericMap data = new NVGenericMap("data");
//            contentConfig.add(data);
//            NVGenericMap attributes = new NVGenericMap("attributes");
//            data.add("type", "profile");
//            data.add(attributes);
//            System.out.println(GSONUtil.toJSONDefault(contentConfig));





            NVGenericMap parameters = new NVGenericMap("Parameters");

            parameters.build(params.asNVPair("email"))
                    .build(params.asNVPair("name"))
                    .build("bogus", "noval");

            if (params.nameExists("last_name"))
            {
                NVPair lastName = params.asNVPair("last_name");
                lastName.setName("data.attributes." + lastName.getName());
                System.out.println(lastName.getName());
                parameters.build(lastName);

            }


//            if ( encoder == null) {
//                encoder = new HTTPNVGMBiEncoder(contentConfig, "data.attributes");
//                String json = GSONUtil.toJSONDefault(encoder);
//                System.out.println(json);
//                encoder = GSONUtil.fromJSONDefault(json, HTTPNVGMBiEncoder.class);
//            }

            if (userAPI == null)
                userAPI = HTTPAPIManager.SINGLETON.buildEndPoint(config, encoder, decoder)
                    .setRateController(new RateController("klaviyo", "75/min"))
                    .setScheduler(TaskUtil.defaultTaskScheduler())
                    .setDomain(domain);
           // System.out.println(GSONUtil.toJSONDefault(new RateController("klavio", "75/min")));
            HTTPCallback<NVGenericMap, NVGenericMap> callback = new HTTPCallback<NVGenericMap, NVGenericMap>(parameters) {
                
                @Override
                public void exception(Throwable e)
                {
                    if (e instanceof HTTPCallException)
                    {
                        HTTPCallException exception = (HTTPCallException) e;


                        if (getEndpoint().lookupPositiveResult(exception.getStatusCode().CODE) != null)
                        {

                            System.out.println("Duplicate user " + get());
                            System.out.println("Error Code:" + exception.getStatusCode());
                            e.printStackTrace();

                        }
                        else
                        {
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void accept(HTTPAPIResult<NVGenericMap> apiResult)
                {
                    System.out.println(apiResult);
                }
            };
            callback.setEndpoint(userAPI);


            userAPI.syncCall(callback, null);
            //System.out.println(userAPI.syncCall(parameters));



            String username = params.stringValue("user", true);
            String password = params.stringValue("password", true);

            if(username != null && password != null)
            {

                HTTPCallback<NVGenericMap, NVGenericMap> loginCallback = new HTTPCallback<NVGenericMap, NVGenericMap>() {

                    @Override
                    public void accept(HTTPAPIResult<NVGenericMap> apiResult)
                    {
                        System.out.println(apiResult);
                    }
                };

                System.out.println("Try to login for " + username);
                HTTPAuthorizationBasic basicAuth = new HTTPAuthorizationBasic(username, password);
                HTTPAPIEndPoint loginEP = HTTPAPIManager.SINGLETON.lookup("api.xlogistx.io.login");
                callback.setEndpoint(loginEP);
                loginEP.syncCall(loginCallback, basicAuth);

            }




            //hmci.getHeaders().add("revision", "2023-07-15");



//            System.out.println(config.getAuthorization().toHTTPHeader());
//            System.out.println(HTTPCall.send(config));



        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        TaskUtil.waitIfBusyThenClose(50);

    }
}
