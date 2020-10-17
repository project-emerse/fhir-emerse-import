import {LoggerConfig} from "@uukmm/ng-logger";
import {NgxLoggerLevel} from "ngx-logger";

const loggerConfig: LoggerConfig = {
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "",
  disableConsoleLogging: true
};


export const environment = {
  production: true,
  serverEndpoint: "http://127.0.0.1/emerse-it-server-prod",
  loggerConfig
};
