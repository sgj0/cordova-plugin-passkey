const exec = require("cordova/exec");

const serializeOptions = (options) =>
  typeof options === "string" ? options : JSON.stringify(options);

module.exports = {
  getPasskey: (options, successCallback, errorCallback) => {
    exec(successCallback, errorCallback, "PasskeyPlugin", "getPasskey", [
      serializeOptions(options),
    ]);
  },

  createPasskey: (options, successCallback, errorCallback) => {
    exec(successCallback, errorCallback, "PasskeyPlugin", "createPasskey", [
      serializeOptions(options),
    ]);
  },
};
