const exec = require("cordova/exec");

module.exports = {
  getPasskey: (optionsJson, successCallback, errorCallback) => {
    exec(successCallback, errorCallback, "PasskeyPlugin", "getPasskey", [
      optionsJson,
    ]);
  },
};
