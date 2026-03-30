import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import ptBR from "./locales/pt-BR.json";
import enUS from "./locales/en-US.json";

const STORAGE_KEY = "db-metrics-lang";
const DEFAULT_LANG = "pt-BR";

const savedLang = localStorage.getItem(STORAGE_KEY);
const initialLang =
  savedLang === "pt-BR" || savedLang === "en-US" ? savedLang : DEFAULT_LANG;

i18n.use(initReactI18next).init({
  resources: {
    "pt-BR": { translation: ptBR },
    "en-US": { translation: enUS },
  },
  lng: initialLang,
  fallbackLng: DEFAULT_LANG,
  interpolation: {
    escapeValue: false,
  },
});

export function setLanguage(lang: "pt-BR" | "en-US") {
  localStorage.setItem(STORAGE_KEY, lang);
  void i18n.changeLanguage(lang);
}

export default i18n;
