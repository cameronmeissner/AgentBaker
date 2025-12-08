package main

import (
	"flag"
	"fmt"
	"log"
	"os"

	internal "github.com/Azure/AgentBaker/vhdbuilder/packer/templater/internal"
	"github.com/Azure/AgentBaker/vhdbuilder/packer/templater/internal/config"
)

type flags struct {
	outputPath string
}

func (f *flags) validate() error {
	if f.outputPath == "" {
		return fmt.Errorf("output-path must be specified")
	}
	return nil
}

var fl = new(flags)

func parseFlags() {
	flag.StringVar(&fl.outputPath, "output-path", "", "where to write the generated packer template")
	flag.Parse()
}

func main() {
	parseFlags()
	if err := fl.validate(); err != nil {
		log.Printf("failed to validate command line flags: %s", err)
		os.Exit(1)
	}
	vhdConfig, err := config.LoadVHDConfigFromEnv()
	if err != nil {
		log.Printf("failed to load VHD configuration from environment: %s", err)
		os.Exit(1)
	}
	packerTemplate, err := internal.BuildPackerTemplate(vhdConfig)
	if err != nil {
		log.Printf("failed to build packer template with VHD configuration: %s", err)
		os.Exit(1)
	}
	if err := os.WriteFile(fl.outputPath, []byte(packerTemplate), os.ModePerm); err != nil {
		log.Printf("failed to write generated packer template to %s: %s", fl.outputPath, err)
		os.Exit(1)
	}
	log.Printf("generated packer template at %s:\n%s\n", fl.outputPath, packerTemplate)
}
