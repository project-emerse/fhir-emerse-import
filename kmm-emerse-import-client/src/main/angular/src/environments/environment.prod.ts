import * as Logger from "ngx-logger";
import {NgxLoggerLevel} from "ngx-logger";

const loggerConfig: Logger.LoggerConfig = {
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "https://edmopencdsdev.med.utah.edu:8443/kmm-client-logger/log",
  disableConsoleLogging: true
};


export const environment = {
  production: true,
  serverEndpoint: "http://127.0.0.1/emerse-it-prod",
  loggerConfig
};
