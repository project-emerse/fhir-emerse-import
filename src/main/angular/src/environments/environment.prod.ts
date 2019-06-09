import {NgxLoggerLevel} from "ngx-logger";
import * as Logger from 'ngx-logger';

const loggerConfig: Logger.LoggerConfig = {
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "https://edmopencdsdev.med.utah.edu:8443/kmm-client-logger/log",
  disableConsoleLogging: true
};


export const environment = {
  production: true,
  loggerConfig
};
