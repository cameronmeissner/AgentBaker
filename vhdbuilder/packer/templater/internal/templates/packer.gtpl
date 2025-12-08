{
  "variables": {
    "subscription_id": "{{ "{{env `AZURE_SUBSCRIPTION_ID`}}" }}",
    "gallery_subscription_id": "{{ "{{user `gallery_subscription_id`}}" }}",
    "location": "{{ "{{env `PACKER_BUILD_LOCATION`}}" }}",
    "vm_size": "{{ "{{env `AZURE_VM_SIZE`}}" }}",
    "build_definition_name": "{{ "{{env `BUILD_DEFINITION_NAME`}}" }}",
    "build_number": "{{ "{{env `BUILD_NUMBER`}}" }}",
    "build_id": "{{ "{{env `BUILD_ID`}}" }}",
    "commit": "{{ "{{env `GIT_VERSION`}}" }}",
    "feature_flags": "{{ "{{env `FEATURE_FLAGS`}}" }}",
    "image_version": "{{ "{{env `IMAGE_VERSION`}}" }}",
    "os_version": "{{ "{{env `OS_VERSION`}}" }}",
    "sku_name": "{{ "{{env `SKU_NAME`}}" }}",
    "hyperv_generation": "{{ "{{env `HYPERV_GENERATION`}}" }}",
    "sig_gallery_name": "{{ "{{env `SIG_GALLERY_NAME`}}" }}",
    "sig_image_name": "{{ "{{env `SIG_IMAGE_NAME`}}" }}",
    "sig_image_version": "{{ "{{env `SIG_IMAGE_VERSION`}}" }}",
    "container_runtime": "{{ "{{env `CONTAINER_RUNTIME`}}" }}",
    "teleportd_plugin_download_url": "{{ "{{env `TELEPORTD_PLUGIN_DOWNLOAD_URL`}}" }}",
    "captured_sig_version": "{{ "{{env `${CAPTURED_SIG_VERSION`}}" }}",
    "enable_fips": "{{ "{{env `ENABLE_FIPS`}}" }}",
    "img_publisher": "{{ "{{env `IMG_PUBLISHER`}}" }}",
    "img_offer": "{{ "{{env `IMG_OFFER`}}" }}",
    "img_sku": "{{ "{{env `IMG_SKU`}}" }}",
    "img_version": "{{ "{{env `IMG_VERSION`}}" }}",
    "vnet_resource_group_name": "{{ "{{env `VNET_RESOURCE_GROUP_NAME`}}" }}",
    "vnet_name": "{{ "{{env `VNET_NAME`}}" }}",
    "subnet_name": "{{ "{{env `SUBNET_NAME`}}" }}",
    "enable_cgroupv2": "{{ "{{env `ENABLE_CGROUPV2`}}" }}",
    "private_packages_url": "{{ "{{env `PRIVATE_PACKAGES_URL`}}" }}",
    "branch": "{{ "{{env `BRANCH`}}" }}",
    "vhd_build_timestamp": "{{ "{{user `VHD_BUILD_TIMESTAMP`}}" }}",
    "local_doca_repo_url": "{{ "{{env `LOCAL_DOCA_REPO_URL`}}" }}",
    "continue_on_local_repo_download_error": "{{ "{{env `CONTINUE_ON_LOCAL_REPO_DOWNLOAD_ERROR`}}" }}"
  },
  "builders": [
    {
      "type": "azure-arm",
      "subscription_id": "{{ "{{user `subscription_id`}}" }}",
      "virtual_network_resource_group_name": "{{ "{{user `vnet_resource_group_name`}}" }}",
      "virtual_network_name": "{{ "{{user `vnet_name`}}" }}",
      "virtual_network_subnet_name": "{{" {{user `subnet_name`}} "}}",
      "ssh_read_write_timeout": "5m",
      "os_type": "Linux",
      "os_disk_size_gb": 30,
      "image_publisher": "{{ "{{user `img_publisher`}}" }}",
      "image_offer": "{{ "{{user `img_offer`}}" }}",
      "image_sku": "{{ "{{user `img_sku`}}" }}",
      "image_version": "{{ "{{user `img_version`}}" }}",
      "azure_tags": {
        "buildDefinitionName": "{{ "{{user `build_definition_name`}}" }}",
        "buildNumber": "{{ "{{user `build_number`}}" }}",
        "buildId":" {{ "{{user `build_id`}}" }}",
        "SkipLinuxAzSecPack": "true",
        "os": "Linux",
        "now": "{{ "{{user `create_time`}}" }}",
        "createdBy": "aks-vhd-pipeline",
        "image_sku":" {{ "{{user `img_sku`}}" }}",
        "branch": "{{ "{{user `branch`}}" }}"
      },
      "location": "{{ "{{user `location`}}" }}",
      "vm_size": "{{ "{{user `vm_size`}}" }}",
      "use_azure_cli_auth": "true",
      "polling_duration_timeout": "1h",
{{- if eq .OS "Flatcar" }}
      "custom_data_file": "./vhdbuilder/packer/flatcar-customdata.json",
{{- end }}
{{- if .FeatureFlags.CVM }}
      "secure_boot_enabled": true,
      "vtpm_enabled": true,
      "security_type": "ConfidentialVM",
      "security_encryption_type": "VMGuestStateOnly",
{{- end }}
      "shared_image_gallery_destination": {
    {{- if .FeatureFlags.CVM }}
        "specialized": true,
        "confidential_vm_image_encryption_type": "EncryptedVMGuestStateOnlyWithPmk",
    {{- end }}
        "subscription": "{{ "{{user `gallery_subscription_id`}}" }}",
        "resource_group": "{{ "{{user `resource_group_name`}}" }}",
        "gallery_name": "{{ "{{user `sig_gallery_name`}}" }}",
        "image_name": "{{ "{{user `sig_image_name`}}" }}",
        "image_version": "{{ "{{user `captured_sig_version`}}" }}",
        "replication_regions": [
          "{{ "{{user `location`}}" }}"
        ]
      },
      "user_assigned_managed_identities": "{{ "{{user `msi_resource_strings`}}" }}"
    }
  ],
  "provisioners": [
    {
      "type": "shell",
      "inline": [
        "sudo mkdir -p /opt/azure/containers",
        "sudo mkdir -p /opt/scripts",
        "sudo mkdir -p /opt/certs"
      ]
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/cse_helpers.sh",
      "destination": "/home/packer/provision_source.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/{{.OS | ToLower}}/cse_helpers_{{.OS | ToLower}}.sh",
      "destination": "/home/packer/provision_source_distro.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/cse_install.sh",
      "destination": "/home/packer/provision_installs.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/{{.OS | ToLower}}/cse_install_{{.OS | ToLower}}.sh",
      "destination": "/home/packer/provision_installs_distro.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/cse_config.sh",
      "destination": "/home/packer/provision_configs.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/cse_start.sh",
      "destination": "/home/packer/provision_start.sh"
    },
    {
      "type": "file",
      "source": "parts/linux/cloud-init/artifacts/cse_benchmark_functions.sh",
      "destination": "/home/packer/provision_source_benchmarks.sh"
    },
    {
      "type": "file",
      "source": "vhdbuilder/scripts/linux/{{.OS | ToLower}}/tool_installs_{{.OS | ToLower}}.sh",
      "destination": "/home/packer/tool_installs_distro.sh"
    },
    {
      "type": "file",
  {{- if eq .OS "Flatcar" }}
      "source": "parts/linux/cloud-init/artifacts/flatcar/update_certs.service"
  {{- else }}
      "source": "parts/linux/cloud-init/artifacts/update_certs.service",
  {{- end }}
      "destination": "/home/packer/update_certs.service"
    },
    {
        "type": "file",
        "direction": "upload",
        "destination": "/home/packer/",
        "sources": [
            "aks-node-controller/bin/aks-node-controller-linux-{{GetArchitectureExtension}}",
            "vhdbuilder/lister/bin/lister",
            "parts/linux/cloud-init/artifacts/aks-node-controller.service",
            "parts/linux/cloud-init/artifacts/cloud-init-status-check.sh",
            "vhdbuilder/packer/prefetch.sh",
            "vhdbuilder/packer/cleanup-vhd.sh",
            "vhdbuilder/packer/packer_source.sh",
            "parts/linux/cloud-init/artifacts/containerd_exec_start.conf",
            "parts/linux/cloud-init/artifacts/kubelet.service",
            "parts/linux/cloud-init/artifacts/secure-tls-bootstrap.service",
            "parts/linux/cloud-init/artifacts/reconcile-private-hosts.sh",
            "parts/linux/cloud-init/artifacts/block_wireserver.sh",
            "parts/linux/cloud-init/artifacts/ensure_imds_restriction.sh",
            "parts/linux/cloud-init/artifacts/measure-tls-bootstrapping-latency.sh",
            "parts/linux/cloud-init/artifacts/measure-tls-bootstrapping-latency.service",
            "parts/linux/cloud-init/artifacts/validate-kubelet-credentials.sh",
            "parts/linux/cloud-init/artifacts/cse_redact_cloud_config.py",
            "parts/linux/cloud-init/artifacts/cse_send_logs.py",
            "parts/linux/cloud-init/artifacts/init-aks-custom-cloud.sh",
            "parts/linux/cloud-init/artifacts/reconcile-private-hosts.service",
            "parts/linux/cloud-init/artifacts/mig-partition.service",
            "parts/linux/cloud-init/artifacts/bind-mount.sh",
            "parts/linux/cloud-init/artifacts/bind-mount.service",
            "parts/linux/cloud-init/artifacts/enable-dhcpv6.sh",
            "parts/linux/cloud-init/artifacts/dhcpv6.service",
            "parts/linux/cloud-init/artifacts/sync-container-logs.sh",
            "parts/linux/cloud-init/artifacts/sync-container-logs.service",
            "parts/linux/cloud-init/artifacts/crictl.yaml",
            "parts/linux/cloud-init/artifacts/ensure-no-dup.sh",
            "parts/linux/cloud-init/artifacts/ensure-no-dup.service",
            "parts/linux/cloud-init/artifacts/teleportd.service",
            "parts/linux/cloud-init/artifacts/setup-custom-search-domains.sh",
            "parts/linux/cloud-init/artifacts/cis.sh",
        {{- if or (eq .OS "Ubuntu") (eq .OS "Flatcar") }}
            "parts/linux/cloud-init/artifacts/ubuntu/ubuntu-snapshot-update.sh",
            "parts/linux/cloud-init/artifacts/ubuntu/snapshot-update.service",
            "parts/linux/cloud-init/artifacts/ubuntu/snapshot-update.timer",
        {{- end }}
        {{- if eq .OS "Mariner" }}
            "parts/linux/cloud-init/artifacts/mariner/mariner-package-update.sh",
            "parts/linux/cloud-init/artifacts/mariner/package-update.service",
            "parts/linux/cloud-init/artifacts/mariner/package-update.timer",
        {{- end }}
            "vhdbuilder/scripts/linux/tool_installs.sh",
            "vhdbuilder/packer/pre-install-dependencies.sh",
            "vhdbuilder/packer/install-dependencies.sh",
            "vhdbuilder/packer/post-install-dependencies.sh",
            "parts/common/components.json",
            "parts/linux/cloud-init/artifacts/manifest.json",
            "parts/linux/cloud-init/artifacts/sysctl-d-60-CIS.conf",
        {{- if and .FIPS (eq .OSVersion "22.04") }}
            "parts/linux/cloud-init/artifacts/sshd_config_2204_fips",
        {{- else }}
            "parts/linux/cloud-init/artifacts/sshd_config",
        {{- end }}
            "parts/linux/cloud-init/artifacts/rsyslog-d-60-CIS.conf",
            "parts/linux/cloud-init/artifacts/logrotate-d-rsyslog-CIS.conf",
            "parts/linux/cloud-init/artifacts/etc-issue",
            "parts/linux/cloud-init/artifacts/etc-issue.net",
            "parts/linux/cloud-init/artifacts/modprobe-CIS.conf",
            "parts/linux/cloud-init/artifacts/faillock-CIS.conf",
            "parts/linux/cloud-init/artifacts/pwquality-CIS.conf",
            "parts/linux/cloud-init/artifacts/pam-d-su",
        {{- if eq .OS "Mariner" }}
            "parts/linux/cloud-init/artifacts/mariner/pam-d-system-auth",
            "parts/linux/cloud-init/artifacts/mariner/pam-d-system-password",
        {{- else}}
            "parts/linux/cloud-init/artifacts/pam-d-common-account",
        {{- end }}
        {{- if or (eq .OSVersion "22.04") (eq .OSVersion "24.04") }}
            "parts/linux/cloud-init/artifacts/pam-d-common-auth-2204",
        {{- else }}
            "parts/linux/cloud-init/artifacts/pam-d-common-auth",
        {{- end }}
            "parts/linux/cloud-init/artifacts/pam-d-common-password",
            "parts/linux/cloud-init/artifacts/profile-d-cis.sh",
            "parts/linux/cloud-init/artifacts/disk_queue.service",
            "parts/linux/cloud-init/artifacts/cgroup-memory-telemetry.sh",
            "parts/linux/cloud-init/artifacts/cgroup-memory-telemetry.service",
            "parts/linux/cloud-init/artifacts/cgroup-memory-telemetry.timer",
            "parts/linux/cloud-init/artifacts/cgroup-pressure-telemetry.sh",
            "parts/linux/cloud-init/artifacts/cgroup-pressure-telemetry.service",
            "parts/linux/cloud-init/artifacts/cgroup-pressure-telemetry.timer",
            "parts/linux/cloud-init/artifacts/update_certs.path",
            "parts/linux/cloud-init/artifacts/update_certs.sh",
            "parts/linux/cloud-init/artifacts/ci-syslog-watcher.path",
            "parts/linux/cloud-init/artifacts/ci-syslog-watcher.service",
            "parts/linux/cloud-init/artifacts/ci-syslog-watcher.sh",
            "parts/linux/cloud-init/artifacts/aks-diagnostic.py",
            "parts/linux/cloud-init/artifacts/aks-log-collector.sh",
            "parts/linux/cloud-init/artifacts/aks-log-collector-send.py",
            "parts/linux/cloud-init/artifacts/aks-log-collector.service",
            "parts/linux/cloud-init/artifacts/aks-log-collector.slice",
            "parts/linux/cloud-init/artifacts/aks-log-collector.timer",
            "parts/linux/cloud-init/artifacts/aks-check-network.sh",
            "parts/linux/cloud-init/artifacts/aks-check-network.service",
            "parts/linux/cloud-init/artifacts/aks-logrotate.sh",
            "parts/linux/cloud-init/artifacts/aks-logrotate.service",
            "parts/linux/cloud-init/artifacts/aks-logrotate.timer",
            "parts/linux/cloud-init/artifacts/aks-logrotate-override.conf",
            "parts/linux/cloud-init/artifacts/aks-rsyslog",
            "parts/linux/cloud-init/artifacts/ipv6_nftables",
            "parts/linux/cloud-init/artifacts/ipv6_nftables.service",
            "parts/linux/cloud-init/artifacts/ipv6_nftables.sh",
            "parts/linux/cloud-init/artifacts/apt-preferences",
            "parts/linux/cloud-init/artifacts/kms.service",
            "parts/linux/cloud-init/artifacts/mig-partition.sh",
            "parts/linux/cloud-init/artifacts/docker_clear_mount_propagation_flags.conf",
            "parts/linux/cloud-init/artifacts/nvidia-modprobe.service",
            "parts/linux/cloud-init/artifacts/nvidia-docker-daemon.json",
            "vhdbuilder/notice_flatcar.txt"
            "vhdbuilder/notice.txt",
            "parts/linux/cloud-init/artifacts/localdns.sh",
            "parts/linux/cloud-init/artifacts/localdns.service",
            "parts/linux/cloud-init/artifacts/localdns-delegate.conf",
            "parts/linux/cloud-init/artifacts/10_azure_nvidia",
            "parts/linux/cloud-init/artifacts/51-azure-nvidia.cfg",
        {{- if and (and (eq .OS "Ubuntu") (eq .OSVersion "24.04")) .FeatureFlags.GB200 }}
            "parts/linux/cloud-init/artifacts/ubuntu/doca.list",
            "parts/linux/cloud-init/artifacts/ubuntu/doca.pub",
            "parts/linux/cloud-init/artifacts/ubuntu/nvidia-2404.list",
            "parts/linux/cloud-init/artifacts/ubuntu/nvidia.pub",
            "parts/linux/cloud-init/artifacts/ubuntu/containerd-nvidia.toml",
            "parts/linux/cloud-init/artifacts/ubuntu/modprobe-nvidia-parameters.conf",
            "vhdbuilder/packer/gb200-mai-bom.json"
        {{- end }}
        ]
    },
    {
      "type": "shell",
      "environment_vars": [
    {{- if and (eq .OS "Ubuntu") (or .FIPS .FeatureFlags.CVM) }}
        "UA_TOKEN={{ "{{user `ua_token`}}" }}",
    {{- end }}
        "ENABLE_CGROUPV2={{ "{{user `enable_cgroupv2`}}" }}",
        "FEATURE_FLAGS={{ "{{user `feature_flags`}}" }}",
        "BUILD_NUMBER={{ "{{user `build_number`}}" }}",
        "BUILD_ID={{ "{{user `build_id`}}" }}",
        "COMMIT={{ "{{user `commit`}}" }}",
        "HYPERV_GENERATION={{ "{{user `hyperv_generation`}}" }}",
        "CONTAINER_RUNTIME={{ "{{user `container_runtime`}}" }}",
        "TELEPORTD_PLUGIN_DOWNLOAD_URL={{ "{{user `teleportd_plugin_download_url`}}" }}",
        "ENABLE_FIPS={{ "{{user `enable_fips`}}" }}",
        "IMG_SKU={{ "{{user `img_sku`}}" }}",
        "VHD_BUILD_TIMESTAMP={{ "{{user `vhd_build_timestamp`}}" }}"
      ],
      "inline": [
        "sudo -E /bin/bash -ux /home/packer/pre-install-dependencies.sh"
      ]
    },
    {
      "type": "shell",
      "inline": "{{GetRebootCommand}}",
      "expect_disconnect": true,
      "skip_clean": true,
      "pause_after": "{{GetRebootPauseDuration}}"
    },
    {
      "type": "shell",
      "environment_vars": [
        "ENABLE_CGROUPV2={{ "{{user `enable_cgroupv2`}}" }}",
        "FEATURE_FLAGS={{ "{{user `feature_flags`}}" }}",
        "BUILD_NUMBER={{ "{{user `build_number`}}" }}",
        "BUILD_ID={{ "{{user `build_id`}}" }}",
        "COMMIT={{ "{{user `commit`}}" }}",
        "HYPERV_GENERATION={{ "{{user `hyperv_generation`}}" }}",
        "CONTAINER_RUNTIME={{ "{{user `container_runtime`}}" }}",
        "TELEPORTD_PLUGIN_DOWNLOAD_URL={{ "{{user `teleportd_plugin_download_url`}}" }}",
        "ENABLE_FIPS={{ "{{user `enable_fips`}}" }}",
        "IMG_SKU={{ "{{user `img_sku`}}" }}",
        "PRIVATE_PACKAGES_URL={{" {{user `private_packages_url`}}" }}",
        "CONTINUE_ON_LOCAL_REPO_DOWNLOAD_ERROR={{ "{{user `continue_on_local_repo_download_error`}}" }}",
        "LOCAL_DOCA_REPO_URL={{" {{user `local_doca_repo_url`}}" }}",
        "VHD_BUILD_TIMESTAMP={{ "{{user `vhd_build_timestamp`}}" }}"
      ],
      "inline": [
        "sudo -E /bin/bash -ux /home/packer/install-dependencies.sh"
      ]
    },
    {
      "type": "file",
      "direction": "download",
      "source": "/var/log/bcc_installation.log",
      "destination": "bcc-tools-installation.log"
    },
    {
      "type": "shell",
      "inline": [
        "sudo rm /var/log/bcc_installation.log"
      ]
    },
    {
      "type": "shell",
      "inline": "{{GetRebootCommand}}",
      "expect_disconnect": true,
      "skip_clean": true,
      "pause_after": "{{GetRebootPauseDuration}}"
    },
    {
      "type": "shell",
      "environment_vars": [
        "ENABLE_CGROUPV2={{ "{{user `enable_cgroupv2`}}" }}",
        "FEATURE_FLAGS={{" {{user `feature_flags`}}" }}",
        "BUILD_NUMBER={{ "{{user `build_number`}}" }}",
        "BUILD_ID={{ "{{user `build_id`}}" }}",
        "COMMIT={{ "{{user `commit`}}" }}",
        "HYPERV_GENERATION={{ "{{user `hyperv_generation`}}" }}",
        "CONTAINER_RUNTIME={{ "{{user `container_runtime`}}" }}",
        "TELEPORTD_PLUGIN_DOWNLOAD_URL={{ "{{user `teleportd_plugin_download_url`}}" }}",
        "ENABLE_FIPS={{ "{{user `enable_fips`}}" }}",
        "IMG_SKU={{ "{{user `img_sku`}}" }}"
      ],
      "inline": [
        "sudo -E /bin/bash -ux /home/packer/post-install-dependencies.sh"
      ]
    },
    {
      "type": "file",
      "source": "vhdbuilder/packer/list-images.sh",
      "destination": "/home/packer/list-images.sh"
    },
    {
      "type": "shell",
      "environment_vars": [
        "SKU_NAME={{ "{{user `sku_name`}}" }}",
        "IMAGE_VERSION={{ "{{user `image_version`}}" }}",
        "CONTAINER_RUNTIME={{ "{{user `container_runtime`}}" }}"
      ],
      "inline": [
        "sudo -E /bin/bash -ux /home/packer/list-images.sh"
      ]
    },
    {
      "type": "file",
      "direction": "download",
      "source": "/opt/azure/containers/image-bom.json",
      "destination": "image-bom.json"
    },
    {
      "type": "file",
      "direction": "download",
      "source": "/opt/azure/vhd-install.complete",
      "destination": "release-notes.txt"
    },
    {
      "type": "file",
      "direction": "download",
      "source": "/opt/azure/vhd-build-performance-data.json",
      "destination": "vhd-build-performance-data.json"
    },
    {
      "type": "file",
      "direction": "download",
      "source": "/opt/azure/vhd-grid-compatibility-data.json",
      "destination": "vhd-grid-compatibility-data.json"
    },
    {
      "type": "shell",
      "inline": [
        "sudo rm /opt/azure/vhd-build-performance-data.json",
        "sudo rm /opt/azure/vhd-grid-compatibility-data.json"
      ]
    },
    {
      "type": "shell",
      "inline": [
        "sudo /bin/bash -eux /home/packer/cis.sh",
        "sudo /bin/bash -eux /opt/azure/containers/cleanup-vhd.sh",
    {{- if eq .OS "Flatcar" }}
        "sudo touch /boot/flatcar/first_boot"
    {{- end }}
        "sudo {{GetWAAgentPath}} -force -deprovision+user && export HISTSIZE=0 && sync || exit 125"
      ]
    }
  ]
}
