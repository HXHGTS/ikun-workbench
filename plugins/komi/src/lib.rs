use serde::{Deserialize, Serialize};
use tauri::{
    plugin::{Builder, PluginApi, TauriPlugin},
    AppHandle, Runtime,
};

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "ai.ikunattacker.komi";

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShowArgs {
    pub title: String,
    pub text: String,
    pub priority: String,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateArgs {
    pub text: String,
    pub progress: i32,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ResultArgs {
    pub ok: bool,
    pub text: String,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SaveArgs {
    pub name: String,
    pub mime: String,
    pub base64: String,
}

#[tauri::command]
async fn show<R: Runtime>(app: AppHandle<R>, args: ShowArgs) -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        let handle = app.plugin_handle(PLUGIN_IDENTIFIER).map_err(|e| e.to_string())?;
        handle.run_mobile_plugin("show", args).map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
async fn update<R: Runtime>(app: AppHandle<R>, args: UpdateArgs) -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        let handle = app.plugin_handle(PLUGIN_IDENTIFIER).map_err(|e| e.to_string())?;
        handle.run_mobile_plugin("update", args).map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
async fn hide<R: Runtime>(app: AppHandle<R>) -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        let handle = app.plugin_handle(PLUGIN_IDENTIFIER).map_err(|e| e.to_string())?;
        handle.run_mobile_plugin("hide", serde_json::json!({})).map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
async fn result<R: Runtime>(app: AppHandle<R>, args: ResultArgs) -> Result<(), String> {
    #[cfg(target_os = "android")]
    {
        let handle = app.plugin_handle(PLUGIN_IDENTIFIER).map_err(|e| e.to_string())?;
        handle.run_mobile_plugin("result", args).map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
async fn save<R: Runtime>(app: AppHandle<R>, args: SaveArgs) -> Result<serde_json::Value, String> {
    #[cfg(target_os = "android")]
    {
        let handle = app.plugin_handle(PLUGIN_IDENTIFIER).map_err(|e| e.to_string())?;
        let r = handle.run_mobile_plugin("save", args).map_err(|e| e.to_string())?;
        Ok(serde_json::to_value(r).unwrap_or(serde_json::json!({})))
    }
    #[cfg(not(target_os = "android"))]
    { Ok(serde_json::json!({})) }
}

pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("komi")
        .invoke_handler(tauri::generate_handler![show, update, hide, result, save])
        .build()
}
