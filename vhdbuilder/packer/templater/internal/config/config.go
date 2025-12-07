package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

type VHD struct {
	OS               string
	OSVersion        string
	HyperVGeneration string
	Architecture     string
	CGroupsV2        bool
	FIPS             bool
	TrustedLaunch    bool
	FeatureFlags     FeatureFlags
}

type FeatureFlags struct {
	CVM   bool
	Kata  bool
	GB200 bool
}

func LoadVHDConfigFromEnv() (VHD, error) {
	os, err := getOSFromEnv()
	if err != nil {
		return VHD{}, err
	}
	osVersion, err := getOSVersionFromEnv()
	if err != nil {
		return VHD{}, err
	}
	hyperVGeneration, err := getHyperVGenerationFromEnv()
	if err != nil {
		return VHD{}, err
	}
	architecture, err := getArchitectureFromEnv()
	if err != nil {
		return VHD{}, err
	}
	cgroupsV2, err := getCGroupsV2FromEnv()
	if err != nil {
		return VHD{}, err
	}
	fips, err := getFIPSFromEnv()
	if err != nil {
		return VHD{}, err
	}
	trustedLaunch, err := getTrustedLaunchFromEnv()
	if err != nil {
		return VHD{}, err
	}
	return VHD{
		OS:               os,
		OSVersion:        osVersion,
		HyperVGeneration: hyperVGeneration,
		Architecture:     architecture,
		CGroupsV2:        cgroupsV2,
		FIPS:             fips,
		TrustedLaunch:    trustedLaunch,
		FeatureFlags:     getFeatureFlagsFromEnv(),
	}, nil
}

func getOSFromEnv() (string, error) {
	os := os.Getenv("OS_SKU")
	if os == "" {
		return "", fmt.Errorf("environment variable OS_SKU must be set")
	}
	switch strings.ToLower(os) {
	case "ubuntu":
		return "Ubuntu", nil
	case "cblmariner", "azurelinux", "azurelinuxosguard":
		return "Mariner", nil
	case "flatcar":
		return "Flatcar", nil
	default:
		return "", fmt.Errorf("unrecognized OS: %s", os)
	}
}

func getOSVersionFromEnv() (string, error) {
	osVersion := os.Getenv("OS_VERSION")
	if osVersion == "" {
		return "", fmt.Errorf("environment variable OS_VERSION must be set")
	}
	return osVersion, nil
}

func getHyperVGenerationFromEnv() (string, error) {
	hyperVGeneration := os.Getenv("HYPERV_GENERATION")
	if hyperVGeneration == "" {
		return "", fmt.Errorf("environment variable HYPERV_GENERATION must be set")
	}
	switch strings.ToLower(hyperVGeneration) {
	case "v1", "v2":
		return hyperVGeneration, nil
	default:
		return "", fmt.Errorf("unrecognized hyperV generation: %s", hyperVGeneration)
	}
}

func getArchitectureFromEnv() (string, error) {
	architecture := os.Getenv("ARCHITECTURE")
	if architecture == "" {
		return "", fmt.Errorf("environment variable ARCHITECTURE must be set")
	}
	switch strings.ToLower(architecture) {
	case "x86_64", "arm64":
		return architecture, nil
	default:
		return "", fmt.Errorf("unrecognized architecture: %s", architecture)
	}
}

func getCGroupsV2FromEnv() (bool, error) {
	cgroupsV2 := os.Getenv("ENABLE_CGROUPV2")
	if cgroupsV2 == "" {
		return false, nil
	}
	return strconv.ParseBool(cgroupsV2)
}

func getFIPSFromEnv() (bool, error) {
	fips := os.Getenv("ENABLE_FIPS")
	if fips == "" {
		return false, nil
	}
	return strconv.ParseBool(fips)
}

func getTrustedLaunchFromEnv() (bool, error) {
	trustedLaunch := os.Getenv("ENABLE_TRUSTED_LAUNCH")
	if trustedLaunch == "" {
		return false, nil
	}
	return strconv.ParseBool(trustedLaunch)
}

func getFeatureFlagsFromEnv() FeatureFlags {
	featureFlags := os.Getenv("FEATURE_FLAGS")
	if featureFlags == "" {
		return FeatureFlags{}
	}
	flags := strings.ToLower(featureFlags)
	return FeatureFlags{
		CVM:   strings.Contains(flags, "cvm"),
		Kata:  strings.Contains(flags, "kata"),
		GB200: strings.Contains(flags, "gb200"),
	}
}
