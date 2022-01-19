# -*- mode: ruby -*-
# vi: set ft=ruby :

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

  config.vm.synced_folder ".", "/vagrant", type: "rsync"
  config.vm.synced_folder File.expand_path('~/.m2'), "/home/vagrant/.m2", create: true, disabled: true
end
