const exec = require("cordova/exec");

module.exports = {
  getPasskey: (options, successCallback, errorCallback) => {
    exec(successCallback, errorCallback, "PasskeyPlugin", "getPasskey", [
      options,
    ]);
  },
};
