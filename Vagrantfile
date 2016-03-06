# -*- mode: ruby -*-
# vi: set ft=ruby :

license_filename = File.dirname(__FILE__) + '/server.license'
unless File.exist?(license_filename)
  puts "`server.license' is missing, create a textfile with the license key."
  puts "E.g. echo 'license_key: xxxxx-xxxxx-xxxxx-xxxxx-xxxxx-xxxxx-xxxxxxxxxx' > server.license"
  exit(1)
end

license = File.read(license_filename)

Vagrant.configure(2) do |config|
  config.vm.box = "debian/contrib-jessie64"
  config.vm.box_version = "8.3.0"

  config.vm.provider :virtualbox do |vb|
    vb.memory = 2048
    vb.cpus = 2
  end

  config.vm.network "forwarded_port", guest: 80,   host: 8080
  config.vm.network "forwarded_port", guest: 1935, host: 1935
  config.vm.network "forwarded_port", guest: 8083, host: 8083
  config.vm.network "forwarded_port", guest: 8086, host: 8086
  config.vm.network "forwarded_port", guest: 8087, host: 8087
  config.vm.network "forwarded_port", guest: 8088, host: 8088
  config.vm.synced_folder ".", "/vagrant"

  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook  = "ansible/playbook.yml"
    ansible.raw_arguments = "--extra-vars license='#{license}'"
  end
end
