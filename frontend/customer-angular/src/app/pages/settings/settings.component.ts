import { Component } from "@angular/core";

@Component({
  selector: "app-settings",
  templateUrl: "./settings.component.html",
  styleUrls: ["./settings.component.css"]
})
export class SettingsComponent {
  useAuthToken: boolean = false;
  bearerToken: string = "";

  constructor() {
    try {
      const lsUse = localStorage.getItem("secure.useAuthToken");
      const lsToken = localStorage.getItem("secure.bearerToken");
      if (lsUse !== null) this.useAuthToken = lsUse === "true";
      if (lsToken !== null) this.bearerToken = lsToken;
    } catch {}
  }

  save() {
    try {
      localStorage.setItem("secure.useAuthToken", String(this.useAuthToken));
      localStorage.setItem("secure.bearerToken", this.bearerToken || "");
      alert("Settings saved");
    } catch (e) {
      console.error("Failed to save settings", e);
    }
  }

  clear() {
    this.useAuthToken = false;
    this.bearerToken = "";
    try {
      localStorage.removeItem("secure.useAuthToken");
      localStorage.removeItem("secure.bearerToken");
    } catch {}
  }
}
