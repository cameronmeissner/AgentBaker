// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package agent

import (
	"context"
	"fmt"

	"github.com/Azure/agentbaker/pkg/agent/datamodel"
	"github.com/Azure/agentbaker/pkg/agent/toggles"
	"github.com/Azure/agentbaker/pkg/agent/vhd/cache"
)

//nolint:revive // Name does not need to be modified to baker
type AgentBaker interface {
	GetNodeBootstrapping(ctx context.Context, config *datamodel.NodeBootstrappingConfiguration) (*datamodel.NodeBootstrapping, error)
	GetLatestSigImageConfig(sigConfig datamodel.SIGConfig, distro datamodel.Distro, envInfo *datamodel.EnvironmentInfo) (*datamodel.SigImageConfig, error)
	GetDistroSigImageConfig(sigConfig datamodel.SIGConfig, envInfo *datamodel.EnvironmentInfo) (map[datamodel.Distro]datamodel.SigImageConfig, error)
	GetCachedVersionsOnVHD() (*datamodel.CachedOnVHD, error)
}

type agentBakerImpl struct {
	toggles *toggles.Toggles
}

var _ AgentBaker = (*agentBakerImpl)(nil)

//nolint:revive // fine to return unexported type due to interface usage
func NewAgentBaker() (*agentBakerImpl, error) {
	return &agentBakerImpl{
		toggles: toggles.New(),
	}, nil
}

func (agentBaker *agentBakerImpl) WithToggles(toggles *toggles.Toggles) *agentBakerImpl {
	agentBaker.toggles = toggles
	return agentBaker
}

//nolint:revive, nolintlint // ctx is not used, but may be in the future
func (agentBaker *agentBakerImpl) GetNodeBootstrapping(ctx context.Context, config *datamodel.NodeBootstrappingConfiguration) (*datamodel.NodeBootstrapping, error) {
	// validate and fix input before passing config to the template generator.
	if config.AgentPoolProfile.IsWindows() {
		validateAndSetWindowsNodeBootstrappingConfiguration(config)
	} else {
		validateAndSetLinuxNodeBootstrappingConfiguration(config)
	}

	templateGenerator := InitializeTemplateGenerator()
	nodeBootstrapping := &datamodel.NodeBootstrapping{
		CustomData: templateGenerator.getNodeBootstrappingPayload(config),
		CSE:        templateGenerator.getNodeBootstrappingCmd(config),
	}

	distro := config.AgentPoolProfile.Distro
	if distro == datamodel.CustomizedWindowsOSImage || distro == datamodel.CustomizedImage || distro == datamodel.CustomizedImageKata {
		return nodeBootstrapping, nil
	}

	osImageConfigMap, hasCloud := datamodel.AzureCloudToOSImageMap[config.CloudSpecConfig.CloudName]
	if !hasCloud {
		return nil, fmt.Errorf("don't have settings for cloud %s", config.CloudSpecConfig.CloudName)
	}

	if osImageConfig, hasImage := osImageConfigMap[distro]; hasImage {
		nodeBootstrapping.OSImageConfig = &osImageConfig
	}

	sigAzureEnvironmentSpecConfig, err := datamodel.GetSIGAzureCloudSpecConfig(config.SIGConfig, config.ContainerService.Location)
	if err != nil {
		return nil, err
	}

	nodeBootstrapping.SigImageConfig = findSIGImageConfig(sigAzureEnvironmentSpecConfig, distro)
	if nodeBootstrapping.SigImageConfig == nil && nodeBootstrapping.OSImageConfig == nil {
		return nil, fmt.Errorf("can't find image for distro %s", distro)
	}

	if !config.AgentPoolProfile.IsWindows() {
		// handle node image version toggle/override
		e := toggles.NewEntityFromNodeBootstrappingConfiguration(config)
		imageVersionOverrides := agentBaker.toggles.GetLinuxNodeImageVersion(e)
		if imageVersion, ok := imageVersionOverrides[string(distro)]; ok {
			nodeBootstrapping.SigImageConfig.Version = imageVersion
		}
	}

	return nodeBootstrapping, nil
}

func (agentBaker *agentBakerImpl) GetLatestSigImageConfig(sigConfig datamodel.SIGConfig,
	distro datamodel.Distro, envInfo *datamodel.EnvironmentInfo) (*datamodel.SigImageConfig, error) {
	sigAzureEnvironmentSpecConfig, err := datamodel.GetSIGAzureCloudSpecConfig(sigConfig, envInfo.Region)
	if err != nil {
		return nil, err
	}

	sigImageConfig := findSIGImageConfig(sigAzureEnvironmentSpecConfig, distro)
	if sigImageConfig == nil {
		return nil, fmt.Errorf("can't find SIG image config for distro %s in region %s", distro, envInfo.Region)
	}

	if !distro.IsWindowsDistro() {
		e := toggles.NewEntityFromEnvironmentInfo(envInfo)
		imageVersionOverrides := agentBaker.toggles.GetLinuxNodeImageVersion(e)
		if imageVersion, ok := imageVersionOverrides[string(distro)]; ok {
			sigImageConfig.Version = imageVersion
		}
	}
	return sigImageConfig, nil
}

func (agentBaker *agentBakerImpl) GetDistroSigImageConfig(
	sigConfig datamodel.SIGConfig, envInfo *datamodel.EnvironmentInfo) (map[datamodel.Distro]datamodel.SigImageConfig, error) {
	allAzureSigConfig, err := datamodel.GetSIGAzureCloudSpecConfig(sigConfig, envInfo.Region)
	if err != nil {
		return nil, fmt.Errorf("failed to get sig image config: %w", err)
	}

	e := toggles.NewEntityFromEnvironmentInfo(envInfo)
	linuxImageVersionOverrides := agentBaker.toggles.GetLinuxNodeImageVersion(e)

	allDistros := map[datamodel.Distro]datamodel.SigImageConfig{}
	for distro, sigConfig := range allAzureSigConfig.SigWindowsImageConfig {
		allDistros[distro] = sigConfig
	}

	for distro, sigConfig := range allAzureSigConfig.SigCBLMarinerImageConfig {
		if version, ok := linuxImageVersionOverrides[string(distro)]; ok {
			sigConfig.Version = version
		}
		allDistros[distro] = sigConfig
	}

	for distro, sigConfig := range allAzureSigConfig.SigAzureLinuxImageConfig {
		if version, ok := linuxImageVersionOverrides[string(distro)]; ok {
			sigConfig.Version = version
		}
		allDistros[distro] = sigConfig
	}

	for distro, sigConfig := range allAzureSigConfig.SigUbuntuImageConfig {
		if version, ok := linuxImageVersionOverrides[string(distro)]; ok {
			sigConfig.Version = version
		}
		allDistros[distro] = sigConfig
	}

	for distro, sigConfig := range allAzureSigConfig.SigUbuntuEdgeZoneImageConfig {
		if version, ok := linuxImageVersionOverrides[string(distro)]; ok {
			sigConfig.Version = version
		}
		allDistros[distro] = sigConfig
	}

	return allDistros, nil
}

func findSIGImageConfig(sigConfig datamodel.SIGAzureEnvironmentSpecConfig, distro datamodel.Distro) *datamodel.SigImageConfig {
	if imageConfig, ok := sigConfig.SigUbuntuImageConfig[distro]; ok {
		return &imageConfig
	}
	if imageConfig, ok := sigConfig.SigCBLMarinerImageConfig[distro]; ok {
		return &imageConfig
	}
	if imageConfig, ok := sigConfig.SigAzureLinuxImageConfig[distro]; ok {
		return &imageConfig
	}
	if imageConfig, ok := sigConfig.SigWindowsImageConfig[distro]; ok {
		return &imageConfig
	}
	if imageConfig, ok := sigConfig.SigUbuntuEdgeZoneImageConfig[distro]; ok {
		return &imageConfig
	}

	return nil
}

func (agentBaker *agentBakerImpl) GetCachedVersionsOnVHD() (*datamodel.CachedOnVHD, error) {
	if cache.FromManifest == nil {
		return nil, fmt.Errorf("cached versions from manifest are not available")
	}
	if cache.FromComponentContainerImages == nil {
		return nil, fmt.Errorf("cached versions from component container images are not available")
	}
	if cache.FromComponentDownloadedFiles == nil {
		return nil, fmt.Errorf("cached versions from component download files are not available")
	}

	return &datamodel.CachedOnVHD{
		FromManifest:                 cache.FromManifest,
		FromComponentContainerImages: cache.FromComponentContainerImages,
		FromComponentDownloadedFiles: cache.FromComponentDownloadedFiles,
	}, nil
}
