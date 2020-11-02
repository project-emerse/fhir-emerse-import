import {LoggerConfig} from "@uukmm/ng-logger";
import {NgxLoggerLevel} from "ngx-logger";

const loggerConfig: LoggerConfig = {
  monitorEnabled: true,
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "",
  disableConsoleLogging: true
};


export const environment = {
  production: true,
  serverEndpoint: "../emerse-it-server-tst",
  loggerConfig
};
