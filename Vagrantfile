# -*- mode: ruby -*-
# vi: set ft=ruby :

def get_bool(name, default)
  val = ENV[name]
  val.nil? ? default :
    case val.downcase
    when "yes", "true", "y", "t", "1" then true
    when "no", "false", "n", "f", "0" then false
    else default
    end
end

Vagrant.configure("2") do |config|

  %w[11.1 11.2 11.3 11.4 12.0 12.1 12.2 12.3].each do |version|
    config.vm.define "freebsd-#{version}" do |instance|
      instance.vm.box = "bento/freebsd-#{version}"
    end

    config.vm.define "freebsd-#{version}-i386" do |instance|
      instance.vm.box = "#{version == "11.2" ? "in-vagranti" : "bento"}/freebsd-#{version}-i386"
    end
  end

  (0..9).each do |minor|
    config.vm.define "openbsd-6.#{minor}" do |instance|
      instance.vm.box = "l3system/openbsd6#{minor}"
    end

    config.vm.define "openbsd-6.#{minor}-i386" do |instance|
      instance.vm.box = "l3system/openbsd6#{minor}-i386"
    end
  end

  config.vm.define "mcandre-openbsd-i386" do |instance|
    instance.vm.box = "mcandre/vagrant-openbsd-gas-i386"
  end

  config.vm.provider "virtualbox" do |v|
    v.cpus = 2
  end

  skip_sync_folder = get_bool("SKIP_SYNC_FOLDER", false)
  # Allow commands such as `vagrant box remove` to be called from other folders
  config.vm.provision "shell", :inline => File.read(File.join(__dir__, ".github", "provision.sh"))
  config.vm.synced_folder __dir__, "/vagrant", type: "rsync", disabled: skip_sync_folder
  config.vm.synced_folder File.expand_path('~/.m2'), "/home/vagrant/.m2", type: "rsync", create: true, disabled: skip_sync_folder
end
