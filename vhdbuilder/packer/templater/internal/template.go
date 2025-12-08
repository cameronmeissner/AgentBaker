package template

import (
	"bytes"
	_ "embed"
	"strings"
	"text/template"

	"github.com/Azure/AgentBaker/vhdbuilder/packer/templater/internal/config"
)

var (
	//go:embed templates/packer.gtpl
	packerTemplate string
)

func BuildPackerTemplate(vhdConfig config.VHD) (string, error) {
	tmpl := template.Must(template.New("packer").Funcs(getFuncMap(vhdConfig)).Parse(packerTemplate))
	var buffer bytes.Buffer
	if err := tmpl.Execute(&buffer, vhdConfig); err != nil {
		return "", err
	}
	return buffer.String(), nil

}

func getFuncMap(vhdConfig config.VHD) template.FuncMap {
	return template.FuncMap{
		"ToLower": strings.ToLower,
		"EnableUbuntuAdvantage": func() bool {
			return strings.EqualFold(vhdConfig.OS, "ubuntu") && (vhdConfig.FIPS || vhdConfig.FeatureFlags.CVM)
		},
		"GetArchitectureExtension": func() string {
			switch strings.ToLower(vhdConfig.Architecture) {
			case "arm64":
				return "arm64"
			default:
				return "amd64"
			}
		},
		"GetRebootCommand": func() string {
			switch strings.ToLower(vhdConfig.OS) {
			case "flatcar":
				return "reboot"
			default:
				return "sudo reboot"
			}
		},
		"GetRebootPauseDuration": func() string {
			switch strings.ToLower(vhdConfig.OS) {
			case "flatcar":
				return "0s"
			default:
				return "60s"
			}
		},
		"GetWAAgentPath": func() string {
			switch strings.ToLower(vhdConfig.OS) {
			case "mariner":
				return "waagent"
			default:
				return "/usr/sbin/waagent"
			}
		},
	}
}
