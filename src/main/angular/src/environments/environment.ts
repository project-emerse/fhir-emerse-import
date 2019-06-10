// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.

import * as Logger from "ngx-logger";
import {NgxLoggerLevel} from "ngx-logger";

const loggerConfig: Logger.LoggerConfig = {
  level: NgxLoggerLevel.DEBUG,
  serverLogLevel: NgxLoggerLevel.OFF,
  serverLoggingUrl: "https://edmopencdsdev.med.utah.edu:8443/kmm-client-logger/log",
  disableConsoleLogging: true
};

export const environment = {
  production: false,
  mock: true,
  loggerConfig
};
