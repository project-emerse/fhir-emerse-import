// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.

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
  production: false,
  serverEndpoint: "http://localhost:9080/emerse-it-server-dev",
  loggerConfig
};
