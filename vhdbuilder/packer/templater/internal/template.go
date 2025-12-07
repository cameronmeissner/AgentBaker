package template

import (
	"bytes"
	_ "embed"
	"fmt"
	"strings"
	"text/template"

	"github.com/Azure/AgentBaker/vhdbuilder/packer/templater/internal/config"
)

var (
	//go:embed templates/packer.gtpl
	packer         string
	packerTemplate = template.Must(template.New("packer").Funcs(getFuncMap()).Parse(packer))
)

func BuildPackerTemplate(vhdConfig config.VHD) (string, error) {
	packerTemplate, err := executePackerTemplate(vhdConfig)
	if err != nil {
		return "", fmt.Errorf("failed to execute packer go template: %w", err)
	}
	return packerTemplate, nil
}

func executePackerTemplate(vhdConfig config.VHD) (string, error) {
	var buffer bytes.Buffer
	if err := packerTemplate.Execute(&buffer, vhdConfig); err != nil {
		return "", err
	}
	return buffer.String(), nil
}

func getFuncMap() template.FuncMap {
	return template.FuncMap{
		"ToLower":                strings.ToLower,
		"GetRebootCommand":       getRebootCommand,
		"GetRebootPauseDuration": getRebootPauseDuration,
		"GetWAAgentPath":         getWAAgentPath,
	}
}

func getRebootCommand(vhdConfig config.VHD) string {
	switch strings.ToLower(vhdConfig.OS) {
	case "flatcar":
		return "reboot"
	default:
		return "sudo reboot"
	}
}

func getRebootPauseDuration(vhdConfig config.VHD) string {
	switch strings.ToLower(vhdConfig.OS) {
	case "flatcar":
		return "0s"
	default:
		return "60s"
	}
}

func getWAAgentPath(vhdConfig config.VHD) string {
	switch strings.ToLower(vhdConfig.OS) {
	case "mariner":
		return "waagent"
	default:
		return "/usr/sbin/waagent"
	}
}
